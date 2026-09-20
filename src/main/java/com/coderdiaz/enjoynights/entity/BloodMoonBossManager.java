package com.coderdiaz.enjoynights.entity;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.event.LunarPhaseEvents;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BloodMoonBossManager {
    public static final String TAG_BOSS = "enjoy_nights:blood_moon_boss";

    private static UUID activeBossUuid = null;
    private static ServerBossEvent bossBar = null;
    private static int roarCooldownTicks = 0;
    private static final Set<Integer> summonedPhases = new HashSet<>();
    private static boolean bossDefeatedThisNight = false;
    private static com.coderdiaz.enjoynights.entity.ai.BloodMoonBossCombatAI combatAI = null;

    public static boolean isBossActive() {
        return activeBossUuid != null;
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

        if (!isBloodMoon) {
            if (activeBossUuid != null) {
                cleanupBossBar();
                activeBossUuid = null;
            }
            bossDefeatedThisNight = false;
            return;
        }

        long dayTime = level.getDayTime();
        long timeOfDay = dayTime % 24000L;

        // Midnight check (tick 18000)
        if (timeOfDay >= 18000L && activeBossUuid == null && !bossDefeatedThisNight && EnjoyNightsConfig.SERVER.bloodMoonBossEnabled.get()) {
            spawnBoss(level);
        }

        if (activeBossUuid != null) {
            Entity entity = level.getEntity(activeBossUuid);
            if (entity instanceof WitherSkeleton boss && boss.isAlive()) {
                // Combat AI Tick (moveset, target commitment, cleave, leap)
                if (combatAI == null) {
                    combatAI = new com.coderdiaz.enjoynights.entity.ai.BloodMoonBossCombatAI(boss);
                }
                combatAI.tick(level);

                // TIME FREEZE: Lock time at midnight (18000)
                if (EnjoyNightsConfig.SERVER.bloodMoonBossFreezeTime.get()) {
                    long midnightDayTime = (dayTime / 24000L) * 24000L + 18000L;
                    if (dayTime != midnightDayTime) {
                        level.setDayTime(midnightDayTime);
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

                // Combat Ability 1: Roar & Darkness Pulse
                roarCooldownTicks--;
                if (roarCooldownTicks <= 0) {
                    executeRoar(level, boss);
                    roarCooldownTicks = EnjoyNightsConfig.SERVER.bloodMoonBossRoarCooldownSeconds.get() * 20;
                }

                // Combat Ability 2: Minion summoning at 75%, 50%, 25% health
                if (EnjoyNightsConfig.SERVER.bloodMoonBossSummonMinions.get()) {
                    float hpRatio = boss.getHealth() / boss.getMaxHealth();
                    if (hpRatio <= 0.75F && !summonedPhases.contains(75)) {
                        summonMinions(level, boss, 2);
                        summonedPhases.add(75);
                    }
                    if (hpRatio <= 0.50F && !summonedPhases.contains(50)) {
                        summonMinions(level, boss, 3);
                        summonedPhases.add(50);
                    }
                    if (hpRatio <= 0.25F && !summonedPhases.contains(25)) {
                        summonMinions(level, boss, 4);
                        boss.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 1, false, true, true));
                        boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 0, false, true, true));
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
                bossDefeatedThisNight = true;
            }
        }
    }

    private static void spawnBoss(ServerLevel level) {
        List<ServerPlayer> players = level.players().stream()
                .filter(p -> !p.isSpectator() && p.isAlive())
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

        // Count nearby players within 64 blocks for scaling
        AABB countBox = new AABB(spawnPos).inflate(64.0);
        long nearbyPlayers = level.players().stream()
                .filter(p -> !p.isSpectator() && p.isAlive() && countBox.contains(p.position()))
                .count();
        long extraPlayers = Math.max(0, nearbyPlayers - 1);

        WitherSkeleton boss = EntityType.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
        if (boss == null) {
            return;
        }

        boss.setPos(sx + 0.5, (double) sy, sz + 0.5);

        // Attributes scaled by players
        double baseHealth = EnjoyNightsConfig.SERVER.bloodMoonBossBaseHealth.get();
        double healthPerPlayer = EnjoyNightsConfig.SERVER.bloodMoonBossHealthPerPlayer.get();
        double totalHealth = baseHealth + extraPlayers * healthPerPlayer;

        double baseDamage = EnjoyNightsConfig.SERVER.bloodMoonBossBaseDamage.get();
        double damagePerPlayer = EnjoyNightsConfig.SERVER.bloodMoonBossDamagePerPlayer.get();
        double totalDamage = baseDamage + extraPlayers * damagePerPlayer;

        double speed = EnjoyNightsConfig.SERVER.bloodMoonBossSpeed.get();
        double knockbackRes = EnjoyNightsConfig.SERVER.bloodMoonBossKnockbackResistance.get();

        AttributeInstance maxHealthAttr = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(totalHealth);
        }
        boss.setHealth((float) totalHealth);

        AttributeInstance attackAttr = boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(totalDamage);
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
        combatAI = new com.coderdiaz.enjoynights.entity.ai.BloodMoonBossCombatAI(boss);
        summonedPhases.clear();
        roarCooldownTicks = EnjoyNightsConfig.SERVER.bloodMoonBossRoarCooldownSeconds.get() * 20;

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

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.translatable("enjoy_nights.boss.spawn", bossNameStr)
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(announceTitle));
            player.connection.send(new ClientboundSetSubtitleTextPacket(announceSubtitle));
        }

        level.playSound(null, sx, sy, sz, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.8F, 0.5F);
        level.playSound(null, sx, sy, sz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 0.6F);
    }

    private static void executeRoar(ServerLevel level, LivingEntity boss) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.6F, 0.7F);

        AABB roarBox = boss.getBoundingBox().inflate(20.0);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, roarBox)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false, true));

            // Small impulse away from boss
            Vec3 dir = player.position().subtract(boss.position()).normalize().scale(0.8);
            player.push(dir.x, 0.3, dir.z);
            player.hurtMarked = true;
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

            BlockPos minionPos = new BlockPos(mx, my, mz);

            if (level.getRandom().nextBoolean()) {
                // Summon Fiery Skeleton
                Skeleton skeleton = EntityType.SKELETON.create(level, EntitySpawnReason.EVENT);
                if (skeleton != null) {
                    skeleton.setPos(mx + 0.5, (double) my, mz + 0.5);
                    skeleton.addTag(LunarPhaseEvents.TAG_POWERED_MOB);
                    skeleton.addTag("enjoy_nights:fire_skeleton");
                    skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET)); // Visual hint

                    ItemStack bow = new ItemStack(Items.BOW);
                    Holder<Enchantment> flameHolder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FLAME);
                    bow.enchant(flameHolder, 1);
                    skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
                    skeleton.setDropChance(EquipmentSlot.HEAD, 0.0F);
                    skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

                    level.addFreshEntity(skeleton);
                }
            } else {
                // Summon Armored Zombie
                Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.EVENT);
                if (zombie != null) {
                    zombie.setPos(mx + 0.5, (double) my, mz + 0.5);
                    zombie.addTag(LunarPhaseEvents.TAG_POWERED_MOB);
                    zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                    zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
                    zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                    level.addFreshEntity(zombie);
                }
            }
        }
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
