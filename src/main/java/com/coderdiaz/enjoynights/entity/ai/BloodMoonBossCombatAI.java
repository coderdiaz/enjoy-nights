package com.coderdiaz.enjoynights.entity.ai;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for Elden Ring-styled boss combat behavior for El Heraldo Carmesí.
 * Handles target commitment and rotation, telegraphed movesets (leap slam, ground shockwave,
 * repulsion burst), conical cleave attacks, and Phase 2 "Furia del Eclipse".
 */
public class BloodMoonBossCombatAI {

    public enum WindupType {
        NONE,
        LEAP,
        SHOCKWAVE
    }

    private final WitherSkeleton boss;

    private int targetCommitmentTicks = 0;
    private int leapCooldown = 120; // Initial delay before first leap
    private int shockwaveCooldown = 160;
    private int repulsionCooldown = 60;
    private int meleeSwingCooldown = 0;

    private int windupTicks = 0;
    private WindupType activeWindup = WindupType.NONE;
    private Vec3 leapTargetPos = null;
    private boolean isInLeap = false;
    private boolean phase2Active = false;

    private final Map<UUID, Float> playerThreat = new ConcurrentHashMap<>();

    public BloodMoonBossCombatAI(WitherSkeleton boss) {
        this.boss = boss;
    }

    public void tick(ServerLevel level) {
        if (!boss.isAlive() || boss.isRemoved()) {
            return;
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

        // 4. Handle active telegraphed windup
        if (windupTicks > 0) {
            windupTicks--;
            boss.getNavigation().stop();
            boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);

            // Emit warning smoke at feet
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

            if (windupTicks == 0) {
                if (activeWindup == WindupType.LEAP) {
                    performLeapLaunch(level);
                } else if (activeWindup == WindupType.SHOCKWAVE) {
                    performShockwaveImpact(level);
                }
                activeWindup = WindupType.NONE;
            }
            return; // Pause actions during telegraph windup
        }

        // 5. Update ability cooldowns
        if (leapCooldown > 0) leapCooldown--;
        if (shockwaveCooldown > 0) shockwaveCooldown--;
        if (repulsionCooldown > 0) repulsionCooldown--;
        if (meleeSwingCooldown > 0) meleeSwingCooldown--;
        if (targetCommitmentTicks > 0) targetCommitmentTicks--;

        // 6. Target rotation and commitment system
        updateTargetCommitment(level);

        Player currentTarget = null;
        if (boss.getTarget() instanceof Player p && p.isAlive() && !p.isSpectator()) {
            currentTarget = p;
        }

        // 7. Ability: Repulsion Wave (Anti-Gank if 3+ players crowd the boss)
        if (repulsionCooldown <= 0) {
            AABB crowdBox = boss.getBoundingBox().inflate(3.5);
            List<Player> nearbyCrowd = level.getEntitiesOfClass(Player.class, crowdBox, pl -> !pl.isSpectator() && pl.isAlive());
            if (nearbyCrowd.size() >= 3) {
                executeRepulsionWave(level, nearbyCrowd);
                return;
            }
        }

        // 8. Ability: Devastating Leap (Anti-Campers / Anti-Archers)
        if (currentTarget != null && EnjoyNightsConfig.SERVER.bloodMoonBossLeapAttackEnabled.get() && leapCooldown <= 0) {
            double dist = boss.distanceTo(currentTarget);
            if (dist >= 7.5 && dist <= 24.0) {
                activeWindup = WindupType.LEAP;
                windupTicks = 15; // 0.75s windup telegraph
                leapTargetPos = currentTarget.position();
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.3F, 1.7F);
                return;
            }
        }

        // 9. Ability: Ground Shockwave / Fissure (Knockup Vertical)
        if (currentTarget != null && shockwaveCooldown <= 0) {
            double dist = boss.distanceTo(currentTarget);
            if (dist <= 4.5) {
                activeWindup = WindupType.SHOCKWAVE;
                windupTicks = 13; // ~0.65s telegraph
                level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2F, 0.5F);
                return;
            }
        }

        // 10. Melee Cleave Attack (Frontal 120-degree cone)
        if (currentTarget != null && EnjoyNightsConfig.SERVER.bloodMoonBossCleaveEnabled.get() && meleeSwingCooldown <= 0) {
            double radius = EnjoyNightsConfig.SERVER.bloodMoonBossCleaveRadius.get();
            if (boss.distanceTo(currentTarget) <= radius) {
                executeCleaveAttack(level, currentTarget, radius);
                meleeSwingCooldown = phase2Active ? 14 : 20; // 30% faster swings in phase 2
            }
        }
    }

    private void updateTargetCommitment(ServerLevel level) {
        if (targetCommitmentTicks > 0 && boss.getTarget() instanceof Player p && p.isAlive() && !p.isSpectator()) {
            return;
        }

        AABB searchBox = boss.getBoundingBox().inflate(48.0);
        List<Player> validPlayers = level.getEntitiesOfClass(Player.class, searchBox, p -> !p.isSpectator() && p.isAlive());

        if (validPlayers.isEmpty()) {
            boss.setTarget(null);
            return;
        }

        // Check if there is an archer/sniper with high threat > 8 blocks away
        Player highestThreatRanged = null;
        float maxThreat = 0;
        for (Player p : validPlayers) {
            float threat = playerThreat.getOrDefault(p.getUUID(), 0.0F);
            if (threat > maxThreat && boss.distanceTo(p) > 8.0) {
                maxThreat = threat;
                highestThreatRanged = p;
            }
        }

        // 60% chance to target the ranged annoyance if threat is significant
        if (highestThreatRanged != null && maxThreat >= 15.0F && level.getRandom().nextFloat() < 0.60F) {
            boss.setTarget(highestThreatRanged);
            playerThreat.put(highestThreatRanged.getUUID(), maxThreat * 0.5F); // diminish threat
        } else {
            // Otherwise target closest player to maintain fluid aggression
            validPlayers.stream()
                    .min(Comparator.comparingDouble(boss::distanceToSqr))
                    .ifPresent(boss::setTarget);
        }

        targetCommitmentTicks = 100; // Commit for 5 seconds to avoid jittering
    }

    private void performLeapLaunch(ServerLevel level) {
        if (leapTargetPos == null) {
            return;
        }

        Vec3 diff = leapTargetPos.subtract(boss.position());
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (horizontalDist < 1.0) {
            horizontalDist = 1.0;
        }

        double speedFactor = Math.min(1.4, Math.max(0.7, horizontalDist * 0.08));
        Vec3 jumpVec = new Vec3(
                diff.x * (speedFactor / horizontalDist),
                0.78,
                diff.z * (speedFactor / horizontalDist)
        );

        boss.setDeltaMovement(jumpVec);
        isInLeap = true;
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.2F, 0.5F);
    }

    private void executeLeapImpact(ServerLevel level) {
        level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 0.2, boss.getZ(), 3, 0.5, 0.2, 0.5, 0.0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 0.2, boss.getZ(), 35, 1.6, 0.2, 1.6, 0.15);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.75F);

        float baseDmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25F;
        AABB impactBox = boss.getBoundingBox().inflate(6.0);
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

            p.hurtServer(level, level.damageSources().mobAttack(boss), baseDmg);

            // Push outward
            Vec3 push = p.position().subtract(boss.position()).normalize().scale(1.2).add(0, 0.35, 0);
            p.setDeltaMovement(push);
            p.hurtMarked = true;
        }

        int cdSec = EnjoyNightsConfig.SERVER.bloodMoonBossLeapCooldownSeconds.get();
        leapCooldown = (phase2Active ? cdSec / 2 : cdSec) * 20;
        targetCommitmentTicks = 0; // Rotate target immediately after slam
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
            p.hurtServer(level, level.damageSources().mobAttack(boss), baseDmg * 0.90F);
            // Knockup into air
            Vec3 motion = p.getDeltaMovement();
            p.setDeltaMovement(motion.x * 0.5, 0.72, motion.z * 0.5);
            p.hurtMarked = true;
        }

        int cdSec = EnjoyNightsConfig.SERVER.bloodMoonBossShockwaveCooldownSeconds.get();
        shockwaveCooldown = (phase2Active ? cdSec / 2 : cdSec) * 20;
        targetCommitmentTicks = 0;
    }

    private void executeRepulsionWave(ServerLevel level, List<Player> nearby) {
        level.sendParticles(ParticleTypes.SONIC_BOOM, boss.getX(), boss.getY() + 1.2, boss.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.0, boss.getZ(), 30, 1.5, 0.5, 1.5, 0.2);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5F, 0.8F);

        float baseDmg = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
        for (Player p : nearby) {
            Vec3 push = p.position().subtract(boss.position()).normalize().scale(1.5).add(0, 0.35, 0);
            p.setDeltaMovement(push);
            p.hurtMarked = true;
            p.hurtServer(level, level.damageSources().mobAttack(boss), baseDmg);
        }

        repulsionCooldown = 120; // 6 seconds
    }

    private void executeCleaveAttack(ServerLevel level, Player primaryTarget, double radius) {
        boss.swing(InteractionHand.MAIN_HAND);

        Vec3 look = boss.getLookAngle();
        double angleDeg = EnjoyNightsConfig.SERVER.bloodMoonBossCleaveAngle.get();
        double cosThreshold = Math.cos(Math.toRadians(angleDeg / 2.0));

        // Visual / Audio
        Vec3 sweepPos = boss.position().add(look.scale(1.8)).add(0, 1.2, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, sweepPos.x, sweepPos.y, sweepPos.z, 2, 0.2, 0.1, 0.2, 0.0);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.3F, 0.6F);

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

    private void activatePhase2(ServerLevel level) {
        phase2Active = true;
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.4F, 1.1F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.5, boss.getZ(), 50, 1.5, 1.5, 1.5, 0.25);

        // Speed and strength buffs
        boss.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 1, false, true, true));
        boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 0, false, true, true));

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
    }

    public boolean isPhase2() {
        return phase2Active;
    }
}
