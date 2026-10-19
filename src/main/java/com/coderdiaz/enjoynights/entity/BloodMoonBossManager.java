package com.coderdiaz.enjoynights.entity;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.event.LunarPhaseEvents;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BloodMoonBossManager {
    public static final String TAG_BOSS = "enjoy_nights:blood_moon_boss";
    /** Tag marking a reinforcement summoned by the boss, distinct from regular siege mobs. */
    public static final String TAG_BOSS_MINION = "enjoy_nights:blood_moon_boss_minion";

    private static UUID activeBossUuid = null;
    private static BlockPos arenaAnchorPos = null;
    private static ServerBossEvent bossBar = null;
    private static int roarCooldownTicks = 0;
    private static int freezeTimeRemainingTicks = 0;
    private static final Set<Integer> summonedPhases = new HashSet<>();
    private static boolean bossDefeatedThisNight = false;
    private static com.coderdiaz.enjoynights.entity.ai.BloodMoonBossCombatAI combatAI = null;

    public static boolean isBossActive() {
        return activeBossUuid != null;
    }

    /** The altar position anchoring the current boss arena, or null if no boss is active. */
    @Nullable
    public static BlockPos getArenaAnchor() {
        return arenaAnchorPos;
    }

    /** Configured radius in blocks of the boss battle arena (confinement + targeting). */
    public static int getArenaRadius() {
        return EnjoyNightsConfig.SERVER.bloodMoonBossArenaRadius.get();
    }

    /** Configured radius in blocks around the altar where siege spawns are suppressed. */
    public static int getSuppressRadius() {
        return EnjoyNightsConfig.SERVER.bloodMoonBossArenaSuppressRadius.get();
    }

    /** True when a boss is active and its arena anchor is known. */
    public static boolean isArenaActive() {
        return activeBossUuid != null && arenaAnchorPos != null;
    }

    public static void recordPlayerDamage(UUID playerUuid, float damage) {
        if (combatAI != null) {
            combatAI.recordDamage(playerUuid, damage);
        }
    }

    public static void tick(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        boolean isBloodMoon = NightAndMoonHelper.isBloodMoon(level);
        boolean isNight = NightAndMoonHelper.isNight(level);
        long dayTime = level.getDayTime();
        long timeOfDay = dayTime % 24000L;

        // Dawn or Blood Moon End check: if morning arrived (tick 23500+ or daytime) or Blood Moon has ended
        if (!isBloodMoon || !isNight || timeOfDay >= 23500L) {
            if (activeBossUuid != null) {
                Entity entity = level.getEntity(activeBossUuid);
                if (entity instanceof WitherSkeleton boss && boss.isAlive()) {
                    if (EnjoyNightsConfig.SERVER.bloodMoonBossDespawnAtDawn.get()) {
                        despawnBossOnDawn(level, boss);
                    } else {
                        cleanupBossBar();
                        activeBossUuid = null;
                        clearArenaAnchor(level, false);
                    }
                } else {
                    cleanupBossBar();
                    activeBossUuid = null;
                    clearArenaAnchor(level, false);
                }
            }

            // Safety cleanup for any lingering boss entity in loaded chunks if despawnAtDawn is true
            if (EnjoyNightsConfig.SERVER.bloodMoonBossDespawnAtDawn.get()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof WitherSkeleton boss && boss.isAlive() && boss.getTags().contains(TAG_BOSS)) {
                        despawnBossOnDawn(level, boss);
                    }
                }
            }

            freezeTimeRemainingTicks = 0;
            bossDefeatedThisNight = false;
            return;
        }

        // Midnight check: legacy automatic spawn only if ritual is NOT required
        if (timeOfDay >= 18000L && activeBossUuid == null && !bossDefeatedThisNight && EnjoyNightsConfig.SERVER.bloodMoonBossEnabled.get()) {
            if (!EnjoyNightsConfig.SERVER.bloodMoonBossRequireRitual.get()) {
                spawnBoss(level);
            }
        }

        if (activeBossUuid != null) {
            Entity entity = level.getEntity(activeBossUuid);
            if (entity instanceof WitherSkeleton boss && boss.isAlive()) {
                // Combat AI Tick (moveset, target commitment, cleave, leap, charged lunge)
                if (combatAI == null) {
                    combatAI = new com.coderdiaz.enjoynights.entity.ai.BloodMoonBossCombatAI(boss);
                }
                combatAI.tick(level);

                // END-OF-EVENT CHECK (runs BEFORE the time freeze): if the Blood Moon has
                // concluded for any reason (schedule rolled over, /bloodmoon stop, morning
                // reached) the boss must not survive. The freeze must never keep it alive.
                boolean eventOver = !isBloodMoon || !isNight || timeOfDay >= 23500L;
                if (eventOver) {
                    // Cancel any remaining freeze and let time flow naturally toward dawn.
                    freezeTimeRemainingTicks = 0;
                    if (EnjoyNightsConfig.SERVER.bloodMoonBossDespawnAtDawn.get()) {
                        despawnBossOnDawn(level, boss);
                    } else {
                        cleanupBossBar();
                        activeBossUuid = null;
                        clearArenaAnchor(level, false);
                    }
                    return;
                }

                // TIME FREEZE: Lock time at midnight for up to 10 minutes (freezeDurationSeconds)
                if (EnjoyNightsConfig.SERVER.bloodMoonBossFreezeTime.get() && freezeTimeRemainingTicks > 0) {
                    long midnightDayTime = (dayTime / 24000L) * 24000L + 18000L;
                    if (dayTime != midnightDayTime) {
                        level.setDayTime(midnightDayTime);
                    }
                    freezeTimeRemainingTicks--;

                    if (freezeTimeRemainingTicks == 0) {
                        // 10 minutes elapsed! Broadcast time resume notification
                        Component unfreezeMsg = Component.translatable("enjoy_nights.boss.time_unfrozen")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                        for (ServerPlayer player : level.players()) {
                            player.sendSystemMessage(unfreezeMsg);
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 1.2F, 0.8F);
                        }
                    }
                }

                // Update Boss Bar
                if (bossBar != null) {
                    float ratio = Math.max(0.0F, Math.min(1.0F, boss.getHealth() / boss.getMaxHealth()));
                    bossBar.setProgress(ratio);

                    for (ServerPlayer player : level.players()) {
                        if (player.distanceToSqr(boss) <= 128.0 * 128.0) {
                            if (!bossBar.getPlayers().contains(player)) {
                                bossBar.addPlayer(player);
                            }
                        } else {
                            bossBar.removePlayer(player);
                        }
                    }
                }

                // Combat Ability 1: Roar & Darkness Pulse. Deferred while the combat AI is
                // mid-move (windup/recovery/leap) so the roar never overlaps an attack and
                // stacks crowd-control. The cooldown is only reset once it actually fires.
                roarCooldownTicks--;
                if (roarCooldownTicks <= 0 && (combatAI == null || !combatAI.isBusy())) {
                    executeRoar(level, boss);
                    roarCooldownTicks = EnjoyNightsConfig.SERVER.bloodMoonBossRoarCooldownSeconds.get() * 20;
                }

                // Combat Ability 2: Minion summoning at 75%, 50%, 25% health (scales with group size)
                if (EnjoyNightsConfig.SERVER.bloodMoonBossSummonMinions.get()) {
                    float hpRatio = boss.getHealth() / boss.getMaxHealth();
                    double scaleRadius = EnjoyNightsConfig.SERVER.bloodMoonBossScalingRadius.get();
                    AABB scanArea = new AABB(boss.blockPosition()).inflate(scaleRadius);
                    int nearbyCount = level.getEntitiesOfClass(ServerPlayer.class, scanArea, p -> !p.isSpectator() && !p.isCreative()).size();
                    int groupBonus = Math.max(0, nearbyCount - 1);
                    int maxPerPhase = EnjoyNightsConfig.SERVER.bloodMoonBossMaxMinionsPerPhase.get();

                    if (hpRatio <= 0.75F && !summonedPhases.contains(75)) {
                        summonMinions(level, boss, Math.min(maxPerPhase, 2 + groupBonus));
                        summonedPhases.add(75);
                    }
                    if (hpRatio <= 0.50F && !summonedPhases.contains(50)) {
                        summonMinions(level, boss, Math.min(maxPerPhase, 3 + groupBonus));
                        summonedPhases.add(50);
                    }
                    if (hpRatio <= 0.25F && !summonedPhases.contains(25)) {
                        summonMinions(level, boss, Math.min(maxPerPhase, 3 + groupBonus));
                        boss.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0, false, true, true));
                        summonedPhases.add(25);
                    }
                }
            } else {
                // Boss died or removed
                if (entity != null && (!entity.isAlive() || entity.isRemoved())) {
                    onBossDefeated(level, entity.blockPosition());
                }
                cleanupBossBar();
                activeBossUuid = null;
                freezeTimeRemainingTicks = 0;
                bossDefeatedThisNight = true;
                clearArenaAnchor(level, true);
            }
        }
    }

    /**
     * Invocación mediante ritual del Altar Carmesí.
     * Calcula la dificultad basada en los jugadores dentro del radio configurado (default 1000 bloques).
     */
    public static void spawnBossFromRitual(ServerLevel level, BlockPos pos, @Nullable Player summoner) {
        // Ensure surface position with open sky (NEVER IN CAVES)
        int sx = pos.getX();
        int sz = pos.getZ();
        int sy = pos.getY();
        if (!level.canSeeSky(pos.above())) {
            sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
        }
        BlockPos spawnPos = new BlockPos(sx, sy, sz);

        double scalingRadius = EnjoyNightsConfig.SERVER.bloodMoonBossScalingRadius.get();
        instantiateBoss(level, spawnPos, scalingRadius, summoner, true);
    }

    /**
     * Invocación automática legacy a medianoche.
     */
    private static void spawnBoss(ServerLevel level) {
        List<ServerPlayer> players = level.players().stream()
                .filter(p -> !p.isSpectator() && !p.isCreative() && p.isAlive())
                .toList();

        if (players.isEmpty()) {
            return;
        }

        // Prioritize player outdoors under sky
        ServerPlayer targetPlayer = players.stream()
                .filter(p -> level.canSeeSky(p.blockPosition()))
                .findFirst()
                .orElse(players.get(0));

        double angle = level.getRandom().nextDouble() * 2.0 * Math.PI;
        int sx = Mth.floor(targetPlayer.getX() + Math.cos(angle) * 22.0);
        int sz = Mth.floor(targetPlayer.getZ() + Math.sin(angle) * 22.0);
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
        BlockPos spawnPos = new BlockPos(sx, sy, sz);

        double scalingRadius = EnjoyNightsConfig.SERVER.bloodMoonBossScalingRadius.get();
        instantiateBoss(level, spawnPos, scalingRadius, targetPlayer, false);
    }

    private static void instantiateBoss(ServerLevel level, BlockPos spawnPos, double scalingRadius, @Nullable Player summoner, boolean isRitual) {
        AABB countBox = new AABB(spawnPos).inflate(scalingRadius);
        List<ServerPlayer> nearbyPlayers = level.players().stream()
                .filter(p -> !p.isSpectator() && !p.isCreative() && p.isAlive() && countBox.contains(p.position()))
                .toList();
        long playerCount = Math.max(1, nearbyPlayers.size());
        long extraPlayers = playerCount - 1;

        WitherSkeleton boss = EntityType.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
        if (boss == null) {
            return;
        }

        boss.setPos(spawnPos.getX() + 0.5, (double) spawnPos.getY(), spawnPos.getZ() + 0.5);

        // Attributes: ONLY health scales with player count! Damage is balanced and predictable.
        double baseHealth = EnjoyNightsConfig.SERVER.bloodMoonBossBaseHealth.get();
        double healthPerPlayer = EnjoyNightsConfig.SERVER.bloodMoonBossHealthPerPlayer.get();
        // Diminishing returns: the first N extra players add full health; beyond that,
        // each additional player contributes a reduced fraction so large groups do not
        // face an impossible health wall given their shared DPS.
        int diminishAfter = EnjoyNightsConfig.SERVER.bloodMoonBossHealthDiminishAfterPlayers.get();
        double diminishFactor = EnjoyNightsConfig.SERVER.bloodMoonBossHealthDiminishFactor.get();
        long fullPlayers = Math.min(extraPlayers, diminishAfter);
        long reducedPlayers = Math.max(0, extraPlayers - diminishAfter);
        double totalHealth = baseHealth
                + fullPlayers * healthPerPlayer
                + reducedPlayers * healthPerPlayer * diminishFactor;

        // Base target damage (default 14.0). Netherite Axe adds +9.0 via item attribute modifier.
        double targetDamage = EnjoyNightsConfig.SERVER.bloodMoonBossBaseDamage.get();
        double baseAttackValue = Math.max(1.0, targetDamage - 9.0);

        double speed = EnjoyNightsConfig.SERVER.bloodMoonBossSpeed.get();
        double knockbackRes = EnjoyNightsConfig.SERVER.bloodMoonBossKnockbackResistance.get();

        AttributeInstance maxHealthAttr = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(totalHealth);
        }
        boss.setHealth((float) totalHealth);

        AttributeInstance attackAttr = boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(baseAttackValue);
        }

        AttributeInstance speedAttr = boss.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speed);
        }

        AttributeInstance knockbackAttr = boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(knockbackRes);
        }

        AttributeInstance followAttr = boss.getAttribute(Attributes.FOLLOW_RANGE);
        if (followAttr != null) {
            followAttr.setBaseValue(64.0);
        }

        // Imposing Scale and Step Height (Elden Ring feel)
        double bossScale = EnjoyNightsConfig.SERVER.bloodMoonBossScale.get();
        AttributeInstance scaleAttr = boss.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(bossScale);
        }

        AttributeInstance stepAttr = boss.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr != null) {
            stepAttr.setBaseValue(1.25);
        }

        // Equipment & visual style
        String bossNameStr = EnjoyNightsConfig.SERVER.bloodMoonBossName.get();
        Component bossName = Component.literal("§4§l" + bossNameStr);
        boss.setCustomName(bossName);
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        boss.addTag(TAG_BOSS);
        boss.addTag(LunarPhaseEvents.TAG_POWERED_MOB);

        boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        boss.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        boss.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        boss.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));

        boss.setDropChance(EquipmentSlot.HEAD, 0.0F);
        boss.setDropChance(EquipmentSlot.CHEST, 0.0F);
        boss.setDropChance(EquipmentSlot.LEGS, 0.0F);
        boss.setDropChance(EquipmentSlot.FEET, 0.0F);
        boss.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        level.addFreshEntity(boss);
        activeBossUuid = boss.getUUID();
        // The arena is anchored at the summon position (the Crimson Altar for a ritual
        // summon, or the chosen spawn for the legacy midnight spawn).
        arenaAnchorPos = spawnPos;
        combatAI = new com.coderdiaz.enjoynights.entity.ai.BloodMoonBossCombatAI(boss);
        summonedPhases.clear();
        roarCooldownTicks = EnjoyNightsConfig.SERVER.bloodMoonBossRoarCooldownSeconds.get() * 20;

        // Initialize 10-minute freeze timer (600s * 20 = 12000 ticks)
        freezeTimeRemainingTicks = EnjoyNightsConfig.SERVER.bloodMoonBossFreezeDurationSeconds.get() * 20;
        if (EnjoyNightsConfig.SERVER.bloodMoonBossFreezeTime.get()) {
            long midnightDayTime = (level.getDayTime() / 24000L) * 24000L + 18000L;
            level.setDayTime(midnightDayTime);
        }

        // Boss Bar Setup
        bossBar = new ServerBossEvent(bossName, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        bossBar.setDarkenScreen(true);
        bossBar.setPlayBossMusic(true);
        bossBar.setProgress(1.0F);
        for (ServerPlayer player : level.players()) {
            bossBar.addPlayer(player);
        }

        // Cinematic Announcement
        Component announceTitle = Component.literal("§4§l" + bossNameStr);
        Component announceSubtitle = Component.translatable("enjoy_nights.boss.subtitle")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);

        String summonerName = summoner != null ? summoner.getName().getString() : "un ritual ancestral";
        Component chatMsg = isRitual
                ? Component.translatable("enjoy_nights.boss.summoned_ritual", summonerName, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), playerCount)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                : Component.translatable("enjoy_nights.boss.spawn", bossNameStr)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(chatMsg);

            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(announceTitle));
            player.connection.send(new ClientboundSetSubtitleTextPacket(announceSubtitle));
        }

        level.playSound(null, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.8F, 0.5F);
        level.playSound(null, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 0.6F);
    }

    public static void despawnActiveBoss(ServerLevel level) {
        if (activeBossUuid != null) {
            Entity entity = level.getEntity(activeBossUuid);
            if (entity instanceof WitherSkeleton boss && boss.isAlive()) {
                despawnBossOnDawn(level, boss);
                return;
            }
        }
        for (Entity e : level.getAllEntities()) {
            if (e instanceof WitherSkeleton boss && boss.isAlive() && e.getTags().contains(TAG_BOSS)) {
                despawnBossOnDawn(level, boss);
                return;
            }
        }
        cleanupBossBar();
        activeBossUuid = null;
        freezeTimeRemainingTicks = 0;
        clearArenaAnchor(level, false);
    }

    public static void despawnBossOnDawn(ServerLevel level, WitherSkeleton boss) {
        // Darkness and Blindness fear wave to nearby players
        AABB impactBox = boss.getBoundingBox().inflate(64.0);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, impactBox)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
        }

        // Visuals & Sound: Disappearing into the shadows
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boss.getX(), boss.getY() + 1.0, boss.getZ(), 50, 1.0, 1.5, 1.0, 0.05);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.0, boss.getZ(), 40, 1.0, 1.5, 1.0, 0.1);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1.4F, 0.5F);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 1.5F, 0.6F);

        // Broadcast message
        Component despawnMsg = Component.translatable("enjoy_nights.boss.despawn_dawn")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(despawnMsg);
        }

        // Remove boss and clean state without drops
        boss.discard();

        // Minion cleanup is handled centrally by clearArenaAnchor (called below).

        cleanupBossBar();
        activeBossUuid = null;
        freezeTimeRemainingTicks = 0;
        bossDefeatedThisNight = true;
        clearArenaAnchor(level, false);
    }

    private static void executeRoar(ServerLevel level, LivingEntity boss) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.6F, 0.7F);

        AABB roarBox = boss.getBoundingBox().inflate(20.0);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, roarBox)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
            // Slowness I (not II) so the roar disrupts without hard-locking groups in place.
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, false, true));

            // Gentle impulse away from boss (reduced from 0.8 to avoid oppressive constant knockback)
            Vec3 dir = player.position().subtract(boss.position()).normalize().scale(0.45);
            player.push(dir.x, 0.2, dir.z);
            player.hurtMarked = true;
        }

        // Commander rally: the roar also marshals the boss's own minions — pull nearby
        // ones toward the boss and grant them a short Speed + Strength surge, so the roar
        // reads as a command, not just a crowd-control pulse.
        if (EnjoyNightsConfig.SERVER.bloodMoonBossRoarRallyEnabled.get()) {
            AABB rallyBox = boss.getBoundingBox().inflate(28.0);
            for (Mob minion : level.getEntitiesOfClass(Mob.class, rallyBox,
                    m -> m.isAlive() && m.getTags().contains(TAG_BOSS_MINION))) {
                minion.getNavigation().moveTo(boss.getX(), boss.getY(), boss.getZ(), 1.2);
                minion.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0, false, true, true));
                minion.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0, false, true, true));
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, minion.getX(), minion.getY() + minion.getBbHeight() + 0.3, minion.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
            }
        }
    }

    private static void summonMinions(ServerLevel level, LivingEntity boss, int count) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.2F, 0.6F);

        for (int i = 0; i < count; i++) {
            double angle = level.getRandom().nextDouble() * 2.0 * Math.PI;
            double dist = 4.0 + level.getRandom().nextDouble() * 4.0;
            int mx = Mth.floor(boss.getX() + Math.cos(angle) * dist);
            int mz = Mth.floor(boss.getZ() + Math.sin(angle) * dist);
            int my = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mx, mz);

            // Reinforcement composition: rare illager specialists plus the common core.
            // 5% Evoker, 10% Vindicator, and the remaining 85% split evenly between the
            // Fiery Skeleton and Armored Zombie (so those two stay the most common).
            float roll = level.getRandom().nextFloat();
            if (roll < 0.05F) {
                summonEvoker(level, mx, my, mz);
            } else if (roll < 0.15F) {
                summonVindicator(level, mx, my, mz);
            } else if (level.getRandom().nextBoolean()) {
                summonFierySkeleton(level, mx, my, mz);
            } else {
                summonArmoredZombie(level, mx, my, mz);
            }
        }
    }

    private static void summonFierySkeleton(ServerLevel level, int mx, int my, int mz) {
        Skeleton skeleton = EntityType.SKELETON.create(level, EntitySpawnReason.REINFORCEMENT);
        if (skeleton == null) {
            return;
        }
        skeleton.setPos(mx + 0.5, (double) my, mz + 0.5);
        skeleton.addTag(LunarPhaseEvents.TAG_POWERED_MOB);
        skeleton.addTag(LunarPhaseEvents.TAG_EXTRA_SPAWN);
        skeleton.addTag(TAG_BOSS_MINION);
        skeleton.addTag(LunarPhaseEvents.TAG_FIRE_SKELETON);
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));

        ItemStack bow = new ItemStack(Items.BOW);
        Holder<Enchantment> flameHolder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FLAME);
        bow.enchant(flameHolder, 1);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0F);
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        level.addFreshEntity(skeleton);
    }

    private static void summonArmoredZombie(ServerLevel level, int mx, int my, int mz) {
        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.REINFORCEMENT);
        if (zombie == null) {
            return;
        }
        zombie.setPos(mx + 0.5, (double) my, mz + 0.5);
        zombie.addTag(LunarPhaseEvents.TAG_POWERED_MOB);
        zombie.addTag(LunarPhaseEvents.TAG_EXTRA_SPAWN);
        zombie.addTag(TAG_BOSS_MINION);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        level.addFreshEntity(zombie);
    }

    private static void summonVindicator(ServerLevel level, int mx, int my, int mz) {
        Mob vindicator = EntityType.VINDICATOR.create(level, EntitySpawnReason.REINFORCEMENT);
        if (vindicator == null) {
            return;
        }
        vindicator.setPos(mx + 0.5, (double) my, mz + 0.5);
        vindicator.addTag(LunarPhaseEvents.TAG_POWERED_MOB);
        vindicator.addTag(LunarPhaseEvents.TAG_EXTRA_SPAWN);
        vindicator.addTag(TAG_BOSS_MINION);
        level.addFreshEntity(vindicator);
    }

    private static void summonEvoker(ServerLevel level, int mx, int my, int mz) {
        Mob evoker = EntityType.EVOKER.create(level, EntitySpawnReason.REINFORCEMENT);
        if (evoker == null) {
            return;
        }
        evoker.setPos(mx + 0.5, (double) my, mz + 0.5);
        evoker.addTag(LunarPhaseEvents.TAG_POWERED_MOB);
        evoker.addTag(LunarPhaseEvents.TAG_EXTRA_SPAWN);
        evoker.addTag(TAG_BOSS_MINION);
        level.addFreshEntity(evoker);
    }

    private static void onBossDefeated(ServerLevel level, BlockPos pos) {
        String bossNameStr = EnjoyNightsConfig.SERVER.bloodMoonBossName.get();
        Component defeatedMsg = Component.translatable("enjoy_nights.boss.defeated", bossNameStr)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(defeatedMsg);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        }

        // Drops: Amethyst Shards, Diamonds, Emeralds, Totem of Undying
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.AMETHYST_SHARD, 6 + level.getRandom().nextInt(5)));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.DIAMOND, 3 + level.getRandom().nextInt(3)));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.EMERALD, 2 + level.getRandom().nextInt(3)));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.TOTEM_OF_UNDYING, 1));

        // Experience
        int xp = EnjoyNightsConfig.SERVER.bloodMoonBossExperience.get();
        if (xp > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), xp);
        }
        // Minion cleanup is handled centrally by clearArenaAnchor on the arena-end path.
    }

    /**
     * Removes the Crimson Altar anchoring the arena (the ritual consumes it at the end
     * of the fight, regardless of outcome) and clears the stored anchor. Safe to call
     * when no anchor is set or when the block is no longer an altar. Idempotent.
     *
     * @param victory when true, plays a triumphant consumption effect (the boss was
     *                defeated); when false, a subdued fade effect (dawn / event end)
     */
    private static void clearArenaAnchor(ServerLevel level, boolean victory) {
        // Every arena end path funnels through here, so clear the boss's minions too —
        // this covers the branches that do not go through onBossDefeated/despawnBossOnDawn
        // (e.g. despawnAtDawn disabled, /bloodmoon stop) and the legacy no-anchor spawn.
        clearBossMinions(level);

        BlockPos anchor = arenaAnchorPos;
        arenaAnchorPos = null;
        if (anchor == null) {
            return;
        }
        if (level.getBlockState(anchor).getBlock() instanceof com.coderdiaz.enjoynights.block.CrimsonAltarBlock) {
            double cx = anchor.getX() + 0.5;
            double cy = anchor.getY() + 0.6;
            double cz = anchor.getZ() + 0.5;
            if (victory) {
                // Triumphant release: golden burst and an uplifting chime as the altar is
                // consumed by the completed ritual.
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy, cz, 60, 0.4, 0.6, 0.4, 0.25);
                level.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 25, 0.3, 0.6, 0.3, 0.05);
                level.playSound(null, cx, anchor.getY() + 0.5, cz, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
                level.playSound(null, cx, anchor.getY() + 0.5, cz, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
            } else {
                // Subdued fade: the ritual collapses into smoke at dawn / event end.
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 30, 0.3, 0.5, 0.3, 0.05);
                level.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 20, 0.3, 0.4, 0.3, 0.02);
                level.playSound(null, cx, anchor.getY() + 0.5, cz, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 0.7F);
            }
            level.removeBlock(anchor, false);
        }
    }

    /**
     * Removes every mob summoned as part of the boss fight, anywhere in the level. Uses
     * a global tag scan (not a radius box) so minions that wandered off chasing a fleeing
     * player are still cleaned up, and operates on {@link Mob} rather than {@link Monster}
     * so illager reinforcements (Vindicator, Evoker) are covered regardless of their
     * class hierarchy. Matches the dedicated boss-minion tag so regular siege mobs (which
     * share the generic extra-spawn tag) are left untouched and keep spawning elsewhere.
     */
    private static void clearBossMinions(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob mob
                    && mob.isAlive()
                    && mob.getTags().contains(TAG_BOSS_MINION)
                    && !mob.getTags().contains(TAG_BOSS)) {
                mob.discard();
            }
        }
    }

    private static void cleanupBossBar() {
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
            bossBar = null;
        }
        combatAI = null;
    }
}
