package com.coderdiaz.enjoynights.entity.ai;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.entity.BloodMoonBossManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for Elden Ring-styled boss combat behavior for El Heraldo Carmesí.
 * Handles target commitment and rotation, telegraphed movesets (charged lunge sweep, leap slam,
 * ground shockwave, repulsion burst), punish windows (recovery frames), and Phase 2 "Furia del Eclipse".
 */
public class BloodMoonBossCombatAI {

    public enum WindupType {
        NONE,
        LEAP,
        SHOCKWAVE,
        CHARGED_LUNGE,
        STOMP,
        SKULL_VOLLEY,
        SOUL_PILLARS
    }

    private final WitherSkeleton boss;

    private int targetCommitmentTicks = 0;
    // Gate on threat-based re-targeting so the boss does not swap targets every couple of
    // seconds in a group fight; it may only jump to a high-threat ranged player once per
    // this window.
    private int retargetCooldownTicks = 0;
    private int leapCooldown = 140; // Initial delay before first leap
    private int shockwaveCooldown = 180;
    private int chargedAttackCooldown = 100; // Initial delay before first charged lunge
    private int repulsionCooldown = 60;
    private int stompCooldown = 120;
    private int skullVolleyCooldown = 160;
    private int soulPillarsCooldown = 200;
    private int meleeSwingCooldown = 0;
    private int recoveryTicks = 0;
    private int meleeComboStep = 0;
    // Counts consecutive ticks the boss spends stuck well below the altar level (e.g.
    // lured into a pit). Used to trigger a vertical rescue teleport back to the surface.
    private int belowAnchorTicks = 0;
    // Accumulated horizontal distance since the last heavy footstep effect, so a colossus
    // step (ground shake + deep thud) fires every few blocks of travel.
    private double stepDistanceAccum = 0.0;
    // Short shared lockout between heavy moves (leap/shockwave/charged lunge) so they
    // never chain back-to-back and overwhelm players. Gives a readable rhythm.
    private int globalAbilityCooldown = 0;

    private int windupTicks = 0;
    private WindupType activeWindup = WindupType.NONE;
    private Vec3 leapTargetPos = null;
    private Player chargedLungeTarget = null;
    private java.util.List<Vec3> soulPillarTargets = null;
    private boolean isInLeap = false;
    private boolean phase2Active = false;

    private final Map<UUID, Float> playerThreat = new ConcurrentHashMap<>();
    // Per-player tick of the last forced knockback the boss applied. Used to grant a
    // short grace window so two heavy moves in a row cannot chain a player through the
    // air (anti knockback-lock).
    private final Map<UUID, Long> lastForcedKnockTick = new ConcurrentHashMap<>();
    private static final long KNOCKBACK_GRACE_TICKS = 20L; // ~1s immunity to re-knock

    public BloodMoonBossCombatAI(WitherSkeleton boss) {
        this.boss = boss;
    }

    public void tick(ServerLevel level) {
        if (!boss.isAlive() || boss.isRemoved()) {
            return;
        }

        // 0. Colossus footsteps: accumulate ground travel and emit a heavy step (ground
        // shake particles + a deep thud) every few blocks, so the boss reads as a massive
        // creature rather than a scaled-up skeleton. Only while actually on the ground.
        if (boss.onGround()) {
            Vec3 m = boss.getDeltaMovement();
            stepDistanceAccum += Math.sqrt(m.x * m.x + m.z * m.z);
            if (stepDistanceAccum >= 2.2) {
                stepDistanceAccum = 0.0;
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boss.getX(), boss.getY() + 0.05, boss.getZ(), 6, 0.5, 0.02, 0.5, 0.01);
                level.sendParticles(ParticleTypes.CRIT, boss.getX(), boss.getY() + 0.05, boss.getZ(), 5, 0.6, 0.02, 0.6, 0.05);
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.IRON_GOLEM_STEP, SoundSource.HOSTILE, 1.1F, 0.6F);
            }
        }

        // 1. Health check: Phase 2 "Furia del Eclipse" (< 40% HP)
        if (!phase2Active && EnjoyNightsConfig.SERVER.bloodMoonBossPhase2Enabled.get()) {
            float hpRatio = boss.getHealth() / boss.getMaxHealth();
            if (hpRatio <= 0.40F) {
                activatePhase2(level);
            }
        }

        // 2. Ambient Phase 2 particle trail
        if (phase2Active && level.getRandom().nextFloat() < 0.65F) {
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    boss.getX(),
                    boss.getY() + 1.2,
                    boss.getZ(),
                    2,
                    0.3,
                    0.5,
                    0.3,
                    0.02
            );
        }

        // 3. Handle active leap in progress
        if (isInLeap) {
            // Subtle in-air homing: nudge horizontal velocity toward the live target so
            // the leap tracks a fleeing player without looking teleport-guided.
            Player leapTarget = (chargedLungeTarget != null && chargedLungeTarget.isAlive())
                    ? chargedLungeTarget
                    : (boss.getTarget() instanceof Player tp && tp.isAlive() ? tp : null);
            if (leapTarget != null) {
                Vec3 toTarget = leapTarget.position().subtract(boss.position());
                Vec3 flatDir = new Vec3(toTarget.x, 0, toTarget.z);
                if (flatDir.lengthSqr() > 0.01) {
                    flatDir = flatDir.normalize();
                    Vec3 move = boss.getDeltaMovement();
                    double horizSpeed = Math.sqrt(move.x * move.x + move.z * move.z);
                    // Blend current heading with the target direction (gentle correction).
                    Vec3 newHoriz = new Vec3(move.x, 0, move.z).normalize().scale(0.82)
                            .add(flatDir.scale(0.18)).normalize().scale(horizSpeed);
                    boss.setDeltaMovement(newHoriz.x, move.y, newHoriz.z);
                    leapTargetPos = leapTarget.position();
                }
                // Telegraph the predicted impact zone on the ground so players can scatter.
                level.sendParticles(ParticleTypes.FLAME,
                        leapTarget.getX(), leapTarget.getY() + 0.05, leapTarget.getZ(),
                        6, 1.2, 0.05, 1.2, 0.0);
            }

            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    boss.getX(),
                    boss.getY() + 0.5,
                    boss.getZ(),
                    3,
                    0.2,
                    0.2,
                    0.2,
                    0.01
            );

            if (boss.onGround() && boss.getDeltaMovement().y <= 0.08) {
                executeLeapImpact(level);
                isInLeap = false;
                return;
            }
            return; // Pause other actions while flying through the air
        }

        // 4. Handle recovery pause (Punish window for players after heavy boss moves)
        if (recoveryTicks > 0) {
            recoveryTicks--;
            boss.getNavigation().stop();
            boss.setDeltaMovement(boss.getDeltaMovement().x * 0.4, boss.getDeltaMovement().y, boss.getDeltaMovement().z * 0.4);

            // Keep facing the target so the recovery reads as a wind-down, not a freeze.
            if (boss.getTarget() instanceof Player rp && rp.isAlive()) {
                boss.getLookControl().setLookAt(rp.getX(), rp.getEyeY(), rp.getZ(), 30.0F, 30.0F);
            }

            if (recoveryTicks % 4 == 0) {
                level.sendParticles(
                        ParticleTypes.SMOKE,
                        boss.getX(),
                        boss.getY() + 0.8,
                        boss.getZ(),
                        1,
                        0.15,
                        0.15,
                        0.15,
                        0.01
                );
            }
            return; // Free window for players to counterattack
        }

        // 5. Handle active telegraphed windup
        if (windupTicks > 0) {
            windupTicks--;
            boss.getNavigation().stop();
            boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);

            if (activeWindup == WindupType.CHARGED_LUNGE) {
                // Dynamic tracking: constantly rotate and face the target player during charge
                if (chargedLungeTarget != null && chargedLungeTarget.isAlive()) {
                    boss.getLookControl().setLookAt(chargedLungeTarget.getX(), chargedLungeTarget.getEyeY(), chargedLungeTarget.getZ(), 60.0F, 60.0F);
                    Vec3 diff = chargedLungeTarget.position().subtract(boss.position());
                    float targetYaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0F;
                    boss.setYRot(targetYaw);
                    boss.setYHeadRot(targetYaw);
                    boss.yBodyRot = targetYaw;
                }

                // Periodic sword adjustment animation
                if (windupTicks % 6 == 0) {
                    boss.swing(InteractionHand.MAIN_HAND);
                }

                // Buildup particles
                level.sendParticles(ParticleTypes.ENCHANT, boss.getX(), boss.getY() + 1.2, boss.getZ(), 4, 0.4, 0.6, 0.4, 0.5);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.2, boss.getZ(), 2, 0.3, 0.4, 0.3, 0.02);
                level.sendParticles(ParticleTypes.CRIT, boss.getX(), boss.getY() + 0.2, boss.getZ(), 3, 0.3, 0.1, 0.3, 0.04);
            } else {
                // Warning smoke at feet for leap / shockwave
                level.sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        boss.getX(),
                        boss.getY() + 0.1,
                        boss.getZ(),
                        2,
                        0.25,
                        0.05,
                        0.25,
                        0.01
                );

                // Ground AoE telegraph: outline the danger zone so players can read and dodge it.
                double telegraphRadius = activeWindup == WindupType.SHOCKWAVE ? 5.0
                        : EnjoyNightsConfig.SERVER.bloodMoonBossLeapRadius.get();
                Vec3 center = activeWindup == WindupType.LEAP && leapTargetPos != null
                        ? leapTargetPos : boss.position();
                for (int a = 0; a < 10; a++) {
                    double ang = (Math.PI * 2 / 10) * a;
                    double rx = center.x + Math.cos(ang) * telegraphRadius;
                    double rz = center.z + Math.sin(ang) * telegraphRadius;
                    level.sendParticles(ParticleTypes.FLAME, rx, center.y + 0.05, rz, 1, 0.0, 0.0, 0.0, 0.0);
                }

                // Soul Pillars: outline each marked eruption site so players can clear out.
                if (activeWindup == WindupType.SOUL_PILLARS && soulPillarTargets != null) {
                    for (Vec3 site : soulPillarTargets) {
                        for (int a = 0; a < 8; a++) {
                            double ang = (Math.PI * 2 / 8) * a;
                            double rx = site.x + Math.cos(ang) * 1.6;
                            double rz = site.z + Math.sin(ang) * 1.6;
                            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, rx, site.y + 0.05, rz, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                }

                // Skull Volley: charge-up motes gathering at the boss's head.
                if (activeWindup == WindupType.SKULL_VOLLEY) {
                    level.sendParticles(ParticleTypes.SMOKE, boss.getX(), boss.getEyeY(), boss.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getEyeY(), boss.getZ(), 4, 0.3, 0.3, 0.3, 0.01);
                }
            }

            if (windupTicks == 0) {
                if (activeWindup == WindupType.LEAP) {
                    performLeapLaunch(level);
                } else if (activeWindup == WindupType.SHOCKWAVE) {
                    performShockwaveImpact(level);
                } else if (activeWindup == WindupType.CHARGED_LUNGE) {
                    performChargedLungeStrike(level);
                } else if (activeWindup == WindupType.STOMP) {
                    performStomp(level);
                } else if (activeWindup == WindupType.SKULL_VOLLEY) {
                    performSkullVolley(level);
                } else if (activeWindup == WindupType.SOUL_PILLARS) {
                    performSoulPillars(level);
                }
                activeWindup = WindupType.NONE;
            }
            return; // Pause actions during telegraph windup
        }

        // 6. Update ability cooldowns
        if (leapCooldown > 0) leapCooldown--;
        if (shockwaveCooldown > 0) shockwaveCooldown--;
        if (chargedAttackCooldown > 0) chargedAttackCooldown--;
        if (repulsionCooldown > 0) repulsionCooldown--;
        if (stompCooldown > 0) stompCooldown--;
        if (skullVolleyCooldown > 0) skullVolleyCooldown--;
        if (soulPillarsCooldown > 0) soulPillarsCooldown--;
        if (meleeSwingCooldown > 0) meleeSwingCooldown--;
        if (globalAbilityCooldown > 0) globalAbilityCooldown--;
        if (targetCommitmentTicks > 0) targetCommitmentTicks--;
        if (retargetCooldownTicks > 0) retargetCooldownTicks--;

        // 7. Arena confinement: the boss is bound to the Crimson Altar anchor. If it is
        // dragged beyond the arena it is pulled back; if no player remains inside the
        // arena it returns to the altar and slowly regenerates (reset-style penalty for
        // fleeing). Both run before targeting so an out-of-arena chase cannot start.
        if (handleArenaBinding(level)) {
            return; // Arena is empty: boss is returning/regenerating, skip offensive logic.
        }

        // 8. Target rotation and commitment system
        updateTargetCommitment(level);

        Player currentTarget = null;
        if (boss.getTarget() instanceof Player p && p.isAlive() && !p.isSpectator() && !p.isCreative()) {
            currentTarget = p;
        }

        // 8. Ability: Repulsion Wave (Anti-Gank if 3+ players crowd the boss)
        if (repulsionCooldown <= 0) {
            AABB crowdBox = boss.getBoundingBox().inflate(3.5);
            List<Player> nearbyCrowd = level.getEntitiesOfClass(Player.class, crowdBox, pl -> !pl.isSpectator() && !pl.isCreative() && pl.isAlive());
            if (nearbyCrowd.size() >= 3) {
                executeRepulsionWave(level, nearbyCrowd);
                return;
            }
        }

        // 9. Melee-first bias at close range: if the target is within melee reach and the
        // basic strike is ready, give it a real chance to fire instead of always ceding
        // to a heavy move. This brings back ordinary strikes and breaks the "only lunges"
        // feel. The remaining probability falls through to the heavy-move selector below.
        double cleaveRadius = EnjoyNightsConfig.SERVER.bloodMoonBossCleaveRadius.get();
        boolean inMeleeRange = currentTarget != null && boss.distanceTo(currentTarget) <= cleaveRadius;
        if (inMeleeRange && meleeSwingCooldown <= 0) {
            double meleeBias = EnjoyNightsConfig.SERVER.bloodMoonBossMeleeBias.get();
            if (globalAbilityCooldown > 0 || level.getRandom().nextDouble() < meleeBias) {
                executeMeleeAttack(level, currentTarget, cleaveRadius);
                meleeSwingCooldown = phase2Active ? 26 : 34; // ~1.3s to 1.7s cadence
                return;
            }
        }

        // 10. Heavy-move selection: gather every heavy ability that is eligible right now
        // (by distance band and its own cooldown) and pick ONE at random, instead of always
        // taking the first in a fixed order. This removes the predictable/erratic feel while
        // preserving each move's range identity. All heavies share the global lockout.
        if (currentTarget != null && globalAbilityCooldown <= 0) {
            double dist = boss.distanceTo(currentTarget);
            List<WindupType> eligible = new java.util.ArrayList<>(3);

            if (EnjoyNightsConfig.SERVER.bloodMoonBossLeapAttackEnabled.get()
                    && leapCooldown <= 0 && dist >= 8.0 && dist <= 24.0) {
                eligible.add(WindupType.LEAP);
            }
            if (EnjoyNightsConfig.SERVER.bloodMoonBossChargedAttackEnabled.get()
                    && chargedAttackCooldown <= 0 && dist >= 2.5 && dist <= 8.5) {
                eligible.add(WindupType.CHARGED_LUNGE);
            }
            if (shockwaveCooldown <= 0 && dist <= 4.0) {
                eligible.add(WindupType.SHOCKWAVE);
            }
            // Colossus Stomp: close-range weight move.
            if (EnjoyNightsConfig.SERVER.bloodMoonBossStompEnabled.get()
                    && stompCooldown <= 0 && dist <= 5.0) {
                eligible.add(WindupType.STOMP);
            }
            // Wither Skull volley: mid/long-range dodgeable ranged attack.
            if (EnjoyNightsConfig.SERVER.bloodMoonBossSkullVolleyEnabled.get()
                    && skullVolleyCooldown <= 0 && dist >= 10.0 && dist <= 40.0) {
                eligible.add(WindupType.SKULL_VOLLEY);
            }
            // Soul Fire Pillars: zone-denial at any non-hugging range.
            if (EnjoyNightsConfig.SERVER.bloodMoonBossSoulPillarsEnabled.get()
                    && soulPillarsCooldown <= 0 && dist >= 6.0) {
                eligible.add(WindupType.SOUL_PILLARS);
            }

            if (!eligible.isEmpty()) {
                WindupType choice = eligible.get(level.getRandom().nextInt(eligible.size()));
                beginHeavyMove(level, choice, currentTarget);
                return;
            }
        }

        // 11. Fallback melee: if no heavy fired and the target is in reach, strike. This
        // covers the case where the melee-bias roll above fell through to a heavy that was
        // not actually eligible (e.g. on cooldown).
        if (inMeleeRange && meleeSwingCooldown <= 0) {
            executeMeleeAttack(level, currentTarget, cleaveRadius);
            meleeSwingCooldown = phase2Active ? 26 : 34;
        }
    }

    /**
     * Begins the telegraphed windup for a chosen heavy move: sets the windup type/timer,
     * captures the target, plays the move's telegraph audio, and applies the shared heavy
     * lockout. The actual impact runs when the windup elapses (see the windup handler).
     */
    private void beginHeavyMove(ServerLevel level, WindupType type, Player target) {
        globalAbilityCooldown = EnjoyNightsConfig.SERVER.bloodMoonBossAbilityLockoutTicks.get();
        switch (type) {
            case LEAP -> {
                activeWindup = WindupType.LEAP;
                windupTicks = 22; // 1.1s windup telegraph
                leapTargetPos = target.position();
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.3F, 1.7F);
            }
            case CHARGED_LUNGE -> {
                activeWindup = WindupType.CHARGED_LUNGE;
                windupTicks = 30; // 1.5s charge telegraph (clear reaction window)
                chargedLungeTarget = target;
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.3F, 0.8F);
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4F, 0.85F);
            }
            case SHOCKWAVE -> {
                activeWindup = WindupType.SHOCKWAVE;
                windupTicks = 20; // 1.0s telegraph
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2F, 0.5F);
            }
            case STOMP -> {
                activeWindup = WindupType.STOMP;
                windupTicks = 16; // 0.8s quick raise-foot telegraph
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.IRON_GOLEM_STEP, SoundSource.HOSTILE, 1.5F, 0.6F);
            }
            case SKULL_VOLLEY -> {
                activeWindup = WindupType.SKULL_VOLLEY;
                windupTicks = 24; // 1.2s aim telegraph
                chargedLungeTarget = target; // reuse as the volley's aim anchor
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.4F, 0.6F);
            }
            case SOUL_PILLARS -> {
                activeWindup = WindupType.SOUL_PILLARS;
                windupTicks = 24; // 1.2s warning before eruption
                // Mark up to 3 current player positions inside the arena as pillar sites.
                soulPillarTargets = new java.util.ArrayList<>();
                List<Player> inArena = playersInArena(level);
                int count = Math.min(3, inArena.size());
                for (int i = 0; i < count; i++) {
                    soulPillarTargets.add(inArena.get(i).position());
                }
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.4F, 0.6F);
            }
            default -> { /* NONE: nothing to begin */ }
        }
    }

    /**
     * Returns the players currently inside the boss arena (within the configured radius
     * of the altar anchor). Falls back to a boss-centered search when no anchor is set
     * (e.g. the legacy auto-spawn without an altar), preserving the original behavior.
     */
    private List<Player> playersInArena(ServerLevel level) {
        BlockPos anchor = BloodMoonBossManager.getArenaAnchor();
        if (anchor == null) {
            AABB searchBox = boss.getBoundingBox().inflate(48.0);
            return level.getEntitiesOfClass(Player.class, searchBox, p -> !p.isSpectator() && !p.isCreative() && p.isAlive());
        }
        double radius = BloodMoonBossManager.getArenaRadius();
        AABB arenaBox = new AABB(anchor).inflate(radius);
        Vec3 center = Vec3.atCenterOf(anchor);
        double radiusSq = radius * radius;
        return level.getEntitiesOfClass(Player.class, arenaBox,
                p -> !p.isSpectator() && !p.isCreative() && p.isAlive() && p.position().distanceToSqr(center) <= radiusSq);
    }

    /**
     * Enforces the arena leash and the empty-arena reset.
     *
     * <p>If the boss has drifted beyond {@code radius * 1.1} (hysteresis to avoid border
     * ping-pong) it is pulled back toward the altar, teleporting as a last resort when it
     * ends up well outside. If no player remains in the arena, the boss drops its target,
     * walks back to the altar and regenerates health at the configured rate.
     *
     * @return true when the arena is empty (caller should skip offensive logic this tick)
     */
    /**
     * Detects the boss being trapped well below the altar level (e.g. lured into a dug
     * pit it cannot step out of) and teleports it back to the surface once it has been
     * stuck long enough. The target is the natural surface directly above the boss's own
     * column so it pops out of the pit in place; if that is still below the altar, it
     * falls back to the altar top. Resets the counter whenever the boss is at a healthy
     * height, so ordinary terrain dips never trigger it.
     *
     * @return true if a rescue teleport was performed this tick
     */
    private boolean handlePitRescue(ServerLevel level, BlockPos anchor) {
        // How far below the altar counts as "in a pit", and how long it must persist.
        final int depthThreshold = 4;
        final int stuckTicksBeforeRescue = 60; // ~3 seconds

        double belowBy = anchor.getY() - boss.getY();
        if (belowBy < depthThreshold) {
            belowAnchorTicks = 0;
            return false;
        }

        // Only count as "stuck" while it is not making upward progress; a boss that is
        // climbing back up on its own (positive vertical velocity) is not trapped.
        if (boss.getDeltaMovement().y > 0.08) {
            return false;
        }

        belowAnchorTicks++;
        if (belowAnchorTicks < stuckTicksBeforeRescue) {
            return false;
        }

        belowAnchorTicks = 0;

        // Surface above the boss's current column; fall back to the altar top if that is
        // somehow still at or below the altar level (e.g. roofed-over column).
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, boss.getBlockX(), boss.getBlockZ());
        double destX = boss.getX();
        double destZ = boss.getZ();
        double destY = surfaceY;
        if (surfaceY <= anchor.getY()) {
            destX = anchor.getX() + 0.5;
            destZ = anchor.getZ() + 0.5;
            destY = anchor.getY();
        }

        boss.teleportTo(destX, destY, destZ);
        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
        boss.hurtMarked = true;

        level.sendParticles(ParticleTypes.PORTAL, destX, destY + 1.0, destZ, 40, 0.4, 1.0, 0.4, 0.4);
        level.playSound(null, destX, destY, destZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.7F);
        return true;
    }

    private boolean handleArenaBinding(ServerLevel level) {
        BlockPos anchor = BloodMoonBossManager.getArenaAnchor();
        if (anchor == null) {
            return false; // No arena (legacy spawn): unconstrained behavior.
        }

        double radius = BloodMoonBossManager.getArenaRadius();
        Vec3 center = Vec3.atCenterOf(anchor);
        double distSq = boss.position().distanceToSqr(center);

        // Vertical pit rescue: if players trap the boss several blocks below the altar
        // level, it can get boxed in and never climb out. Track how long it stays well
        // below the anchor and, once stuck, teleport it to the natural surface above its
        // current column (or the altar as a fallback) so the fight can continue.
        if (handlePitRescue(level, anchor)) {
            return false; // Just rescued this tick; resume normal behavior next tick.
        }

        // Leash with hysteresis: only correct once clearly past the boundary.
        double leash = radius * 1.1;
        if (distSq > leash * leash) {
            double dist = Math.sqrt(distSq);
            if (dist > radius * 1.6) {
                // Far outside (knocked away / pathing glitch): snap back onto the altar.
                boss.teleportTo(center.x, anchor.getY(), center.z);
                boss.getNavigation().stop();
            } else {
                // Just past the edge: steer back toward the center.
                boss.getNavigation().moveTo(center.x, anchor.getY(), center.z, 1.1);
                Vec3 pull = center.subtract(boss.position()).normalize().scale(0.25);
                boss.setDeltaMovement(boss.getDeltaMovement().add(pull.x, 0, pull.z));
                boss.hurtMarked = true;
            }
        }

        // Empty-arena reset: no players inside → return to altar and regenerate.
        if (playersInArena(level).isEmpty()) {
            boss.setTarget(null);
            if (distSq > 4.0) {
                boss.getNavigation().moveTo(center.x, anchor.getY(), center.z, 1.0);
            } else {
                boss.getNavigation().stop();
            }

            double regenPerSecond = EnjoyNightsConfig.SERVER.bloodMoonBossArenaRegenPerSecond.get();
            if (regenPerSecond > 0.0 && boss.getHealth() < boss.getMaxHealth()) {
                boss.heal((float) (regenPerSecond / 20.0));
                if (level.getRandom().nextFloat() < 0.15F) {
                    level.sendParticles(ParticleTypes.HEART, boss.getX(), boss.getY() + 1.6, boss.getZ(), 1, 0.3, 0.3, 0.3, 0.0);
                }
            }
            return true;
        }

        return false;
    }

    private void updateTargetCommitment(ServerLevel level) {
        if (targetCommitmentTicks > 0 && boss.getTarget() instanceof Player p && p.isAlive() && !p.isSpectator()) {
            return;
        }

        List<Player> validPlayers = playersInArena(level);

        if (validPlayers.isEmpty()) {
            boss.setTarget(null);
            return;
        }

        // Only consider jumping to a high-threat ranged player when the re-target gate is
        // open, so the boss does not swap focus every couple of seconds in a group.
        Player highestThreatRanged = null;
        float maxThreat = 0;
        if (retargetCooldownTicks <= 0) {
            for (Player p : validPlayers) {
                float threat = playerThreat.getOrDefault(p.getUUID(), 0.0F);
                if (threat > maxThreat && boss.distanceTo(p) > 8.0) {
                    maxThreat = threat;
                    highestThreatRanged = p;
                }
            }
        }

        // 60% chance to target the ranged annoyance if threat is significant
        if (highestThreatRanged != null && maxThreat >= 15.0F && level.getRandom().nextFloat() < 0.60F) {
            boss.setTarget(highestThreatRanged);
            playerThreat.put(highestThreatRanged.getUUID(), maxThreat * 0.5F); // diminish threat
            retargetCooldownTicks = 40; // ~2s before another threat-based swap
        } else {
            // Otherwise target closest player to maintain fluid aggression
            validPlayers.stream()
                    .min(Comparator.comparingDouble(boss::distanceToSqr))
                    .ifPresent(boss::setTarget);
        }

        targetCommitmentTicks = 60; // Commit for 3 seconds to avoid jittering while remaining responsive
    }

    /**
     * Applies a forced knockback to a player, but only if they are not within the brief
     * post-knock grace window — so consecutive heavy moves cannot chain a player through
     * the air (anti knockback-lock). The whole vector is scaled by the configurable
     * knockback multiplier. Returns true if the knock was applied.
     */
    private boolean applyControlledKnockback(ServerLevel level, Player player, Vec3 knock) {
        long now = level.getGameTime();
        Long last = lastForcedKnockTick.get(player.getUUID());
        if (last != null && now - last < KNOCKBACK_GRACE_TICKS) {
            return false; // Still in grace: leave the player's motion alone.
        }
        double mult = EnjoyNightsConfig.SERVER.bloodMoonBossKnockbackMultiplier.get();
        Vec3 scaled = new Vec3(knock.x * mult, knock.y * mult, knock.z * mult);
        player.setDeltaMovement(scaled);
        player.hurtMarked = true;
        lastForcedKnockTick.put(player.getUUID(), now);
        return true;
    }

    private void performLeapLaunch(ServerLevel level) {
        if (leapTargetPos == null) {
            return;
        }

        Vec3 diff = leapTargetPos.subtract(boss.position());
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (horizontalDist < 0.5) {
            horizontalDist = 0.5;
        }

        // Ballistic jump: a tall vertical impulse for a clear attack arc (not a hop),
        // and a horizontal velocity sized to actually reach the target's distance. The
        // horizontal component grows with distance so the boss lunges across the gap and
        // lands on the targeted player, forcing the group to scatter.
        double verticalImpulse = 0.95;
        double horizontalSpeed = Math.min(2.4, 0.42 + horizontalDist * 0.14);
        Vec3 jumpVec = new Vec3(
                (diff.x / horizontalDist) * horizontalSpeed,
                verticalImpulse,
                (diff.z / horizontalDist) * horizontalSpeed
        );

        boss.setDeltaMovement(jumpVec);
        boss.hurtMarked = true;
        isInLeap = true;

        // Aim the body at the destination for a committed, readable leap.
        float yaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0F;
        boss.setYRot(yaw);
        boss.yBodyRot = yaw;

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.3F, 0.5F);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    private void executeLeapImpact(ServerLevel level) {
        double radius = EnjoyNightsConfig.SERVER.bloodMoonBossLeapRadius.get();
        double knockback = EnjoyNightsConfig.SERVER.bloodMoonBossLeapKnockback.get();

        level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 0.2, boss.getZ(), 4, 0.6, 0.2, 0.6, 0.0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 0.2, boss.getZ(), 50, radius * 0.3, 0.2, radius * 0.3, 0.18);
        // Expanding ring telegraph of the dispersal wave.
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX(), boss.getY() + 0.3, boss.getZ(), 6, radius * 0.4, 0.1, radius * 0.4, 0.0);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 0.7F);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2F, 1.1F);

        float baseDmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15F;
        AABB impactBox = boss.getBoundingBox().inflate(radius);
        List<Player> victims = level.getEntitiesOfClass(Player.class, impactBox, p -> !p.isSpectator() && p.isAlive());

        for (Player p : victims) {
            if (p.isBlocking()) {
                // Break shield: apply 5-second cooldown and trigger vanilla shield break event
                ItemStack activeItem = p.getUseItem();
                if (!activeItem.isEmpty()) {
                    p.getCooldowns().addCooldown(activeItem, 100);
                }
                p.stopUsingItem();
                level.broadcastEntityEvent(p, (byte) 30);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHIELD_BREAK.value(), SoundSource.PLAYERS, 1.3F, 0.8F);
            }

            // Damage falls off with distance so the epicentre is the dangerous spot.
            double dist = Math.sqrt(p.distanceToSqr(boss));
            double falloff = Math.max(0.35, 1.0 - (dist / (radius + 1.0)));
            p.hurtServer(level, level.damageSources().mobAttack(boss), baseDmg * (float) falloff);

            // Strong outward dispersal wave: push everyone away to force the group to
            // scatter, but gated by the grace window and with a softer vertical so it
            // disperses without launching players into a chained airtime.
            Vec3 push = p.position().subtract(boss.position());
            if (push.lengthSqr() < 0.01) {
                push = new Vec3(level.getRandom().nextDouble() - 0.5, 0, level.getRandom().nextDouble() - 0.5);
            }
            push = push.normalize().scale(knockback).add(0, 0.30, 0);
            applyControlledKnockback(level, p, push);
        }

        int cdSec = EnjoyNightsConfig.SERVER.bloodMoonBossLeapCooldownSeconds.get();
        leapCooldown = (phase2Active ? Math.max(5, cdSec / 2) : cdSec) * 20;
        recoveryTicks = EnjoyNightsConfig.SERVER.bloodMoonBossRecoveryTicks.get();
        targetCommitmentTicks = 0; // Rotate target after slam
    }

    private void performShockwaveImpact(ServerLevel level) {
        boss.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.LAVA, boss.getX(), boss.getY() + 0.2, boss.getZ(), 20, 1.2, 0.2, 1.2, 0.1);
        level.sendParticles(ParticleTypes.CRIT, boss.getX(), boss.getY() + 0.2, boss.getZ(), 25, 1.5, 0.3, 1.5, 0.2);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.5F, 0.6F);

        float baseDmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB box = boss.getBoundingBox().inflate(5.0);
        List<Player> nearby = level.getEntitiesOfClass(Player.class, box, p -> !p.isSpectator() && p.isAlive());

        for (Player p : nearby) {
            p.hurtServer(level, level.damageSources().mobAttack(boss), baseDmg * 0.75F);
            // Softer knockup so it interrupts without stealing control for too long,
            // and gated so it cannot chain off a previous knock.
            Vec3 motion = p.getDeltaMovement();
            applyControlledKnockback(level, p, new Vec3(motion.x * 0.5, 0.45, motion.z * 0.5));
        }

        int cdSec = EnjoyNightsConfig.SERVER.bloodMoonBossShockwaveCooldownSeconds.get();
        shockwaveCooldown = (phase2Active ? Math.max(5, cdSec / 2) : cdSec) * 20;
        recoveryTicks = Math.max(10, EnjoyNightsConfig.SERVER.bloodMoonBossRecoveryTicks.get() - 4);
        // Keep the current target after a shockwave; only the leap deliberately rotates.
    }

    private void performChargedLungeStrike(ServerLevel level) {
        Vec3 forward;
        if (chargedLungeTarget != null && chargedLungeTarget.isAlive()) {
            Vec3 diff = chargedLungeTarget.position().subtract(boss.position());
            forward = new Vec3(diff.x, 0, diff.z).normalize();
        } else {
            Vec3 look = boss.getLookAngle();
            forward = new Vec3(look.x, 0, look.z).normalize();
        }

        // Horizontal dash towards the target
        Vec3 lungeMotion = new Vec3(forward.x * 1.15, 0.20, forward.z * 1.15);
        boss.setDeltaMovement(lungeMotion);
        boss.hurtMarked = true;

        boss.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.5F, 0.65F);

        // Colossus slam at the dash destination instead of a small sweep scratch.
        Vec3 impact = boss.position().add(forward.x * 2.2, 0, forward.z * 2.2);
        spawnColossusSlamFx(level, impact.x, impact.z);

        float heavyDmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15F;
        double reachRadius = 5.0;
        double cosThreshold = Math.cos(Math.toRadians(140.0 / 2.0)); // 140-degree wide sweep

        AABB box = boss.getBoundingBox().inflate(reachRadius);
        List<Player> candidates = level.getEntitiesOfClass(Player.class, box, p -> !p.isSpectator() && p.isAlive());

        for (Player p : candidates) {
            Vec3 toP = p.position().subtract(boss.position());
            if (toP.length() <= reachRadius) {
                Vec3 norm = toP.normalize();
                double dot = forward.x * norm.x + forward.z * norm.z;
                if (dot >= cosThreshold || p == chargedLungeTarget) {
                    if (p.isBlocking() && phase2Active) {
                        ItemStack activeItem = p.getUseItem();
                        if (!activeItem.isEmpty()) {
                            p.getCooldowns().addCooldown(activeItem, 60); // 3s shield break in Phase 2
                        }
                        p.stopUsingItem();
                        level.broadcastEntityEvent(p, (byte) 30);
                        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHIELD_BREAK.value(), SoundSource.PLAYERS, 1.3F, 0.8F);
                    }

                    p.hurtServer(level, level.damageSources().mobAttack(boss), heavyDmg);

                    Vec3 push = p.position().subtract(boss.position()).normalize().scale(1.1).add(0, 0.15, 0);
                    applyControlledKnockback(level, p, push);

                    if (phase2Active) {
                        p.igniteForSeconds(4);
                    }
                }
            }
        }

        int cdSec = EnjoyNightsConfig.SERVER.bloodMoonBossChargedAttackCooldownSeconds.get();
        chargedAttackCooldown = (phase2Active ? Math.max(5, cdSec / 2) : cdSec) * 20;
        recoveryTicks = EnjoyNightsConfig.SERVER.bloodMoonBossRecoveryTicks.get();
        // Keep the current target after a charged lunge; only the leap deliberately rotates.
        chargedLungeTarget = null;
    }

    /**
     * Colossus Stomp: a quick ground slam at the boss's feet. Deals moderate damage and
     * applies a brief Slowness to anyone caught next to its legs, punishing players who
     * cling to the colossus and reinforcing its sense of mass. Short range, low knockback.
     */
    private void performStomp(ServerLevel level) {
        double stompRadius = 4.5;
        spawnColossusSlamFx(level, boss.getX(), boss.getZ());
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.7F, 0.45F);

        float dmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.80F;
        AABB box = boss.getBoundingBox().inflate(stompRadius);
        for (Player p : level.getEntitiesOfClass(Player.class, box, pl -> !pl.isSpectator() && !pl.isCreative() && pl.isAlive())) {
            if (p.distanceToSqr(boss) <= stompRadius * stompRadius) {
                p.hurtServer(level, level.damageSources().mobAttack(boss), dmg);
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, true, true));
                // Low, grounded shove so it reads as weight, not a launch.
                Vec3 shove = p.position().subtract(boss.position()).normalize().scale(0.4).add(0, 0.12, 0);
                applyControlledKnockback(level, p, shove);
            }
        }

        stompCooldown = EnjoyNightsConfig.SERVER.bloodMoonBossStompCooldownSeconds.get() * 20;
        recoveryTicks = Math.max(8, EnjoyNightsConfig.SERVER.bloodMoonBossRecoveryTicks.get() - 10);
    }

    /**
     * Wither Skull volley: launches a small spread of homing wither skulls at the aim
     * target, a dodgeable/blockable ranged threat for when players keep their distance.
     */
    private void performSkullVolley(ServerLevel level) {
        Player aim = (chargedLungeTarget != null && chargedLungeTarget.isAlive())
                ? chargedLungeTarget
                : (boss.getTarget() instanceof Player tp && tp.isAlive() ? tp : null);
        chargedLungeTarget = null;
        if (aim == null) {
            skullVolleyCooldown = EnjoyNightsConfig.SERVER.bloodMoonBossSkullVolleyCooldownSeconds.get() * 20;
            return;
        }

        boss.playSound(SoundEvents.WITHER_SHOOT, 1.4F, 0.7F);
        double ex = boss.getX();
        double ey = boss.getEyeY();
        double ez = boss.getZ();
        int skulls = phase2Active ? 3 : 2;
        for (int i = 0; i < skulls; i++) {
            Vec3 dir = new Vec3(
                    aim.getX() - ex + (level.getRandom().nextDouble() - 0.5) * 1.5,
                    aim.getEyeY() - ey + (level.getRandom().nextDouble() - 0.5) * 0.8,
                    aim.getZ() - ez + (level.getRandom().nextDouble() - 0.5) * 1.5
            ).normalize();
            net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull skull =
                    new net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull(level, boss, dir);
            skull.setPos(ex + dir.x * 1.5, ey + 0.2, ez + dir.z * 1.5);
            level.addFreshEntity(skull);
        }

        skullVolleyCooldown = EnjoyNightsConfig.SERVER.bloodMoonBossSkullVolleyCooldownSeconds.get() * 20;
        recoveryTicks = Math.max(8, EnjoyNightsConfig.SERVER.bloodMoonBossRecoveryTicks.get() - 8);
    }

    /**
     * Soul Fire Pillars: the eruption phase for the sites marked during the windup. Each
     * marked spot bursts with soul fire, dealing damage and igniting players still inside
     * it — the telegraph gave them time to move, so standing in it is the punish.
     */
    private void performSoulPillars(ServerLevel level) {
        if (soulPillarTargets == null || soulPillarTargets.isEmpty()) {
            soulPillarsCooldown = EnjoyNightsConfig.SERVER.bloodMoonBossSoulPillarsCooldownSeconds.get() * 20;
            return;
        }

        double pillarRadius = 2.0;
        float dmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.90F;

        for (Vec3 site : soulPillarTargets) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, site.x, site.y + 0.8, site.z, 60, 0.4, 1.4, 0.4, 0.12);
            level.sendParticles(ParticleTypes.FLAME, site.x, site.y + 0.5, site.z, 30, 0.5, 1.0, 0.5, 0.08);
            level.playSound(null, site.x, site.y, site.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.3F, 0.6F);

            AABB box = new AABB(site.x - pillarRadius, site.y - 1.0, site.z - pillarRadius,
                    site.x + pillarRadius, site.y + 3.0, site.z + pillarRadius);
            for (Player p : level.getEntitiesOfClass(Player.class, box, pl -> !pl.isSpectator() && !pl.isCreative() && pl.isAlive())) {
                p.hurtServer(level, level.damageSources().mobAttack(boss), dmg);
                p.igniteForSeconds(3);
            }
        }

        soulPillarTargets = null;
        soulPillarsCooldown = EnjoyNightsConfig.SERVER.bloodMoonBossSoulPillarsCooldownSeconds.get() * 20;
        recoveryTicks = Math.max(8, EnjoyNightsConfig.SERVER.bloodMoonBossRecoveryTicks.get() - 6);
    }

    private void executeRepulsionWave(ServerLevel level, List<Player> nearby) {
        level.sendParticles(ParticleTypes.SONIC_BOOM, boss.getX(), boss.getY() + 1.2, boss.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.0, boss.getZ(), 30, 1.5, 0.5, 1.5, 0.2);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5F, 0.8F);

        float baseDmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
        for (Player p : nearby) {
            Vec3 push = p.position().subtract(boss.position()).normalize().scale(1.5).add(0, 0.25, 0);
            applyControlledKnockback(level, p, push);
            p.hurtServer(level, level.damageSources().mobAttack(boss), baseDmg);
        }

        repulsionCooldown = 120; // 6 seconds
    }

    /**
     * Heavy-impact visual/audio for the boss's colossal strikes, replacing the tiny
     * player-style sweep "scratch". Spawns a ground burst (explosion, lava specks, crit
     * and soul-fire debris) at the real impact height for a 1.7x colossus, with a weighty
     * ground-smash + golem-strike sound so the hit reads as an overhead slam.
     *
     * @param cx,cz horizontal impact centre; the burst sits at the boss's feet.
     */
    private void spawnColossusSlamFx(ServerLevel level, double cx, double cz) {
        double fy = boss.getY() + 0.1;
        level.sendParticles(ParticleTypes.EXPLOSION, cx, fy + 0.2, cz, 3, 0.5, 0.1, 0.5, 0.0);
        level.sendParticles(ParticleTypes.LAVA, cx, fy, cz, 14, 1.0, 0.2, 1.0, 0.0);
        level.sendParticles(ParticleTypes.CRIT, cx, fy + 0.2, cz, 30, 1.2, 0.3, 1.2, 0.25);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, fy, cz, 20, 1.0, 0.2, 1.0, 0.08);
        level.sendParticles(ParticleTypes.SMOKE, cx, fy, cz, 16, 1.0, 0.15, 1.0, 0.03);
        level.playSound(null, cx, boss.getY(), cz, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.6F, 0.55F);
        level.playSound(null, cx, boss.getY(), cz, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.4F, 0.6F);
    }

    private void executeMeleeAttack(ServerLevel level, Player primaryTarget, double radius) {
        if (EnjoyNightsConfig.SERVER.bloodMoonBossCleaveEnabled.get() && meleeComboStep == 1) {
            // Combo Finisher: Sweeping Cleave
            executeCleaveAttack(level, primaryTarget, radius);
            meleeComboStep = 0;
        } else {
            // Combo Starter: Direct heavy strike
            boss.swing(InteractionHand.MAIN_HAND);
            boss.doHurtTarget(level, primaryTarget);
            maybeApplyWither(primaryTarget);
            level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.7F);
            if (phase2Active) {
                primaryTarget.igniteForSeconds(2);
            }
            meleeComboStep = 1;
        }
    }

    private void executeCleaveAttack(ServerLevel level, Player primaryTarget, double radius) {
        boss.swing(InteractionHand.MAIN_HAND);

        Vec3 look = boss.getLookAngle();
        double angleDeg = EnjoyNightsConfig.SERVER.bloodMoonBossCleaveAngle.get();
        double cosThreshold = Math.cos(Math.toRadians(angleDeg / 2.0));

        // Colossus overhead slam in front of the boss (at its real feet), not a tiny sweep.
        Vec3 impact = boss.position().add(look.x * 1.8, 0, look.z * 1.8);
        spawnColossusSlamFx(level, impact.x, impact.z);

        float baseDmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB box = boss.getBoundingBox().inflate(radius);
        List<Player> candidates = level.getEntitiesOfClass(Player.class, box, p -> !p.isSpectator() && p.isAlive());

        for (Player p : candidates) {
            Vec3 toP = p.position().subtract(boss.position());
            if (toP.length() <= radius) {
                Vec3 norm = toP.normalize();
                double dot = look.x * norm.x + look.z * norm.z;
                if (dot >= cosThreshold || p == primaryTarget) {
                    if (p == primaryTarget) {
                        boss.doHurtTarget(level, p);
                        maybeApplyWither(p);
                    } else {
                        p.hurtServer(level, level.damageSources().mobAttack(boss), baseDmg * 0.70F);
                    }
                    if (phase2Active) {
                        p.igniteForSeconds(4);
                    }
                }
            }
        }
    }

    /**
     * Applies Wither to a struck player only on a configurable probability roll, instead
     * of guaranteeing it every hit. A modest Wither I for 3s keeps it threatening without
     * the old armor-piercing, regen-locking certainty that made melee feel punishing.
     */
    private void maybeApplyWither(Player target) {
        double chance = EnjoyNightsConfig.SERVER.bloodMoonBossWitherOnHitChance.get();
        if (chance <= 0.0) {
            return;
        }
        if (boss.getRandom().nextDouble() < chance) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true, true));
        }
    }

    private void activatePhase2(ServerLevel level) {
        phase2Active = true;
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.4F, 1.1F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.5, boss.getZ(), 50, 1.5, 1.5, 1.5, 0.25);

        // Speed I (+20%) buff for heightened agility (no Strength to avoid one-shots)
        boss.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0, false, true, true));

        // Global message
        String bossNameStr = EnjoyNightsConfig.SERVER.bloodMoonBossName.get();
        Component p2Msg = Component.translatable("enjoy_nights.boss.phase2", bossNameStr)
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD);

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(p2Msg);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.8F, 1.2F);
        }
    }

    public void recordDamage(UUID playerUuid, float damage) {
        playerThreat.merge(playerUuid, damage, Float::sum);
        if (boss.getTarget() instanceof Player current && !current.getUUID().equals(playerUuid)) {
            if (damage >= 8.0F) {
                targetCommitmentTicks = Math.min(targetCommitmentTicks, 30);
            }
        }
    }

    public boolean isPhase2() {
        return phase2Active;
    }

    /**
     * True while the boss is mid-move (telegraph windup, recovery pause, or airborne leap).
     * The manager checks this so its periodic roar never overlaps an active attack, which
     * previously stacked crowd-control and amplified the loss of player agency.
     */
    public boolean isBusy() {
        return windupTicks > 0 || recoveryTicks > 0 || isInLeap;
    }
}
