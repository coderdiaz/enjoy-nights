package com.coderdiaz.enjoynights.event;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.entity.ai.ZombieSiegeBreakGoal;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

public class BloodMoonEvents {
    private static boolean bloodMoonAnnounced = false;
    private static int siegeWaveTickCounter = 0;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Zombie zombie) {
            zombie.goalSelector.addGoal(1, new ZombieSiegeBreakGoal(zombie));
            zombie.targetSelector.addGoal(1, new com.coderdiaz.enjoynights.entity.ai.BloodMoonSiegeTargetGoal(zombie));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (!EnjoyNightsConfig.SERVER.bloodMoonSiegeEnabled.get()) {
            return;
        }

        boolean isBloodMoon = NightAndMoonHelper.isBloodMoon(level);

        if (isBloodMoon && !bloodMoonAnnounced) {
            bloodMoonAnnounced = true;
            announceBloodMoonStart(level);
        } else if (!isBloodMoon && bloodMoonAnnounced) {
            bloodMoonAnnounced = false;
            announceBloodMoonEnd(level);
            if (NightAndMoonHelper.isForceBloodMoon()) {
                NightAndMoonHelper.setForceBloodMoon(false);
            }
        }

        // Controlled Blood Moon siege waves
        if (isBloodMoon && EnjoyNightsConfig.SERVER.bloodMoonSiegeWavesEnabled.get()) {
            siegeWaveTickCounter++;
            int intervalTicks = EnjoyNightsConfig.SERVER.bloodMoonWaveIntervalSeconds.get() * 20;

            if (siegeWaveTickCounter >= intervalTicks) {
                siegeWaveTickCounter = 0;
                spawnControlledSiegeWave(level);
            }
        } else {
            siegeWaveTickCounter = 0;
        }
    }

    private static void spawnControlledSiegeWave(ServerLevel level) {
        int maxNearby = EnjoyNightsConfig.SERVER.bloodMoonMaxNearbyMobs.get();
        RandomSource random = level.getRandom();

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }

            // Cap verification: count existing hostile monsters within 32 blocks
            AABB searchBox = player.getBoundingBox().inflate(32.0);
            List<Monster> nearbyMonsters = level.getEntitiesOfClass(Monster.class, searchBox);

            if (nearbyMonsters.size() >= maxNearby) {
                // Limit reached: do not spawn extra mobs to prevent excessive swarms/lag
                continue;
            }

            // Spawn 1 to 2 siege zombies at a controlled distance (20 to 28 blocks away)
            int mobsToSpawn = Math.min(1 + random.nextInt(2), maxNearby - nearbyMonsters.size());

            for (int i = 0; i < mobsToSpawn; i++) {
                double angle = random.nextFloat() * (float) (2 * Math.PI);
                double distance = 20.0 + random.nextDouble() * 8.0;

                int spawnX = Mth.floor(player.getX() + Math.cos(angle) * distance);
                int spawnZ = Mth.floor(player.getZ() + Math.sin(angle) * distance);
                int spawnY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);

                BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);

                // Ensure reasonable vertical distance from player and valid air block
                if (Math.abs(spawnY - player.getBlockY()) <= 12 && level.getBlockState(spawnPos).isAir()) {
                    Zombie siegeZombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.EVENT);
                    if (siegeZombie != null) {
                        siegeZombie.setPos(spawnX + 0.5, (double) spawnY, spawnZ + 0.5);
                        siegeZombie.setYRot(random.nextFloat() * 360.0F);
                        siegeZombie.addTag(LunarPhaseEvents.TAG_EXTRA_SPAWN);
                        siegeZombie.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.EVENT, null);
                        siegeZombie.setTarget(player);
                        level.addFreshEntity(siegeZombie);
                    }
                }
            }
        }
    }

    private static void announceBloodMoonStart(ServerLevel level) {
        Component message = Component.translatable("enjoy_nights.bloodmoon.warning")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.WITHER_SPAWN,
                    SoundSource.AMBIENT,
                    1.0f,
                    0.6f
            );
        }
    }

    private static void announceBloodMoonEnd(ServerLevel level) {
        Component message = Component.translatable("enjoy_nights.bloodmoon.ended")
                .withStyle(ChatFormatting.GOLD);

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
        }
    }
}
