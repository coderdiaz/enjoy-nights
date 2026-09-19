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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BloodMoonEvents {
    private static boolean bloodMoonAnnounced = false;
    private static int siegeWaveTickCounter = 0;
    private static final Map<UUID, Integer> DARKNESS_PULSE_COOLDOWNS = new ConcurrentHashMap<>();

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

        // Sporadic atmospheric Darkness pulse (torches flicker and darkness closes in for ~1.5s every 2-3 minutes to unnerve player)
        if (isBloodMoon && EnjoyNightsConfig.SERVER.bloodMoonDarknessPulseEnabled.get()) {
            if (level.getGameTime() % 20 == 0) {
                for (ServerPlayer player : level.players()) {
                    if (player.isSpectator() || !player.isAlive()) {
                        continue;
                    }

                    BlockPos pos = player.blockPosition();
                    int highestBlock = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
                    boolean isOutdoors = level.canSeeSky(pos)
                            || level.canSeeSky(pos.above())
                            || pos.getY() >= highestBlock - 3
                            || level.getBrightness(LightLayer.SKY, pos) >= 5;

                    UUID uuid = player.getUUID();
                    // Initial pulse starts quickly (15-25s) so the player notices it right away upon event start / testing,
                    // and then follows the 2 to 3 minute (120 to 180s) interval.
                    int cooldown = DARKNESS_PULSE_COOLDOWNS.computeIfAbsent(
                            uuid,
                            k -> (15 + level.getRandom().nextInt(10)) * 20
                    );

                    cooldown -= 20;
                    if (cooldown <= 0) {
                        if (isOutdoors) {
                            // Apply Darkness effect: 50 ticks (2.5s total: ~1s blend-in, ~0.8s peak blackout, ~0.7s blend-out)
                            // Sudden visual flicker: torches dim and peripheral vision is swallowed by darkness
                            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false, false));

                            level.playSound(
                                    null,
                                    player.getX(),
                                    player.getY(),
                                    player.getZ(),
                                    SoundEvents.WARDEN_HEARTBEAT,
                                    SoundSource.AMBIENT,
                                    0.85F,
                                    0.65F
                            );

                            // Reset cooldown: 120 to 180 seconds (2 to 3 minutes)
                            cooldown = (120 + level.getRandom().nextInt(60)) * 20;
                        } else {
                            // If player is indoors, hold at 0 so it immediately strikes when stepping outdoors
                            cooldown = 0;
                        }
                    }
                    DARKNESS_PULSE_COOLDOWNS.put(uuid, cooldown);
                }
            }
        } else if (!isBloodMoon) {
            if (!DARKNESS_PULSE_COOLDOWNS.isEmpty()) {
                DARKNESS_PULSE_COOLDOWNS.clear();
            }
            for (ServerPlayer player : level.players()) {
                if (player.hasEffect(MobEffects.DARKNESS)) {
                    player.removeEffect(MobEffects.DARKNESS);
                }
            }
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
        Component title = Component.translatable("enjoy_nights.bloodmoon.title")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        Component subtitle = Component.translatable("enjoy_nights.bloodmoon.subtitle")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(title.copy().append(" — ").append(subtitle));

            // Cinematic on-screen title (Dark Souls / Bloodborne style)
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(20, 100, 30));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitle));

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.WITHER_SPAWN,
                    SoundSource.AMBIENT,
                    1.0f,
                    0.5f // deep, menacing rumble
            );
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.BELL_RESONATE,
                    SoundSource.AMBIENT,
                    1.2f,
                    0.45f // heavy, haunting tolling bell
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
