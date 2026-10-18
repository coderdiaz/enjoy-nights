package com.coderdiaz.enjoynights.event;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.entity.BloodMoonBossManager;
import com.coderdiaz.enjoynights.entity.ai.ZombieSiegeBreakGoal;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import com.coderdiaz.enjoynights.ward.WardingSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BloodMoonEvents {
    private static boolean bloodMoonAnnounced = false;
    private static int siegeWaveTickCounter = 0;
    // Counts siege waves fired during the current Blood Moon night; drives escalation.
    private static int siegeWaveCounter = 0;
    private static final Map<UUID, Integer> DARKNESS_PULSE_COOLDOWNS = new ConcurrentHashMap<>();

    // Anticipatory warning latches. Tracked per absolute day number so each warning
    // fires at most once per relevant day, surviving the ~20 ticks/sec event cadence.
    private static final long NO_DAY = -1L;
    private static long lastProcessedDay = NO_DAY;
    private static long bloodMoonDaysWarnedDay = NO_DAY;
    private static long bloodMoonPreDuskWarnedDay = NO_DAY;
    private static long fullMoonPreDuskWarnedDay = NO_DAY;

    // Tracks whether the Full Moon night is currently active, so the end-of-event
    // mob sweep can fire exactly once when it transitions to false (dawn).
    private static boolean fullMoonActive = false;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Cleanup check for lingering night mobs loading from chunks
        if (event.getEntity() instanceof Mob mob) {
            boolean hasExtraTag = mob.getTags().contains(LunarPhaseEvents.TAG_EXTRA_SPAWN);
            boolean hasFireSkeleton = mob.getTags().contains(LunarPhaseEvents.TAG_FIRE_SKELETON);
            boolean hasBossTag = mob.getTags().contains(BloodMoonBossManager.TAG_BOSS);

            if (hasExtraTag || hasFireSkeleton) {
                // If daytime has arrived or if outside Blood Moon for extra siege mobs, discard immediately!
                if (!NightAndMoonHelper.isNight(event.getLevel()) || (!NightAndMoonHelper.isBloodMoon(event.getLevel()) && hasExtraTag)) {
                    mob.discard();
                    event.setCanceled(true);
                    return;
                }
            }

            if (hasBossTag) {
                // If chunk is loading during daytime or outside Blood Moon, discard lingering boss immediately!
                if (!NightAndMoonHelper.isNight(event.getLevel()) || !NightAndMoonHelper.isBloodMoon(event.getLevel())) {
                    if (EnjoyNightsConfig.SERVER.bloodMoonBossDespawnAtDawn.get()) {
                        mob.discard();
                        event.setCanceled(true);
                        return;
                    }
                }
            }

            // Tag-based legacy/stale cleanup: when a chunk reloads and neither lunar
            // event is active, revert empowered natural mobs and remove any remaining
            // event-spawned mobs (including leftovers from older mod versions that use
            // the same tags). Guarded so it never touches mobs mid-event.
            if (EnjoyNightsConfig.SERVER.eventMobCleanupEnabled.get()
                    && !NightAndMoonHelper.isBloodMoon(event.getLevel())
                    && !NightAndMoonHelper.isFullMoon(event.getLevel())) {
                com.coderdiaz.enjoynights.util.EventMobCleanup.Action action =
                        com.coderdiaz.enjoynights.util.EventMobCleanup.cleanupTaggedMob(
                                mob, BloodMoonBossManager.isBossActive(), false);
                if (action == com.coderdiaz.enjoynights.util.EventMobCleanup.Action.DELETED) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        if (event.getEntity() instanceof Zombie zombie) {
            zombie.goalSelector.addGoal(1, new ZombieSiegeBreakGoal(zombie));
            zombie.targetSelector.addGoal(1, new com.coderdiaz.enjoynights.entity.ai.BloodMoonSiegeTargetGoal(zombie));
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getEntity().getTags().contains(BloodMoonBossManager.TAG_BOSS)) {
            if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                BloodMoonBossManager.recordPlayerDamage(player.getUUID(), event.getNewDamage());
            }
        }

        // On-hit status effects from tagged event mobs. Only affect players so
        // event mobs do not debuff each other, and only on the server side.
        if (event.getEntity() instanceof ServerPlayer victim
                && event.getSource().getEntity() instanceof Mob attacker) {
            if (attacker.getTags().contains(LunarPhaseEvents.TAG_CAVE_SPIDER)) {
                int seconds = EnjoyNightsConfig.SERVER.bloodMoonCaveSpiderPoisonSeconds.get();
                int amplifier = EnjoyNightsConfig.SERVER.bloodMoonCaveSpiderPoisonAmplifier.get();
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20, amplifier, false, true, true));
            } else if (attacker.getTags().contains(LunarPhaseEvents.TAG_WITHER_ELITE)) {
                // Elite Wither Skeleton inflicts Wither for a short, threatening window.
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, false, true, true));
            }
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

        // Anticipatory warnings run independently of the siege toggle so Full Moon
        // warnings still work even when the Blood Moon siege mechanic is disabled.
        processAnticipatoryWarnings(level);

        // Full Moon end-of-event mob sweep, also independent of the siege toggle.
        processFullMoonEndSweep(level);

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
            if (!DARKNESS_PULSE_COOLDOWNS.isEmpty()) {
                DARKNESS_PULSE_COOLDOWNS.clear();
            }
            for (ServerPlayer player : level.players()) {
                if (player.hasEffect(MobEffects.DARKNESS)) {
                    player.removeEffect(MobEffects.DARKNESS);
                }
            }
        }

        // Controlled Blood Moon siege waves
        if (isBloodMoon && EnjoyNightsConfig.SERVER.bloodMoonSiegeWavesEnabled.get()) {
            siegeWaveTickCounter++;
            int intervalTicks = EnjoyNightsConfig.SERVER.bloodMoonWaveIntervalSeconds.get() * 20;

            if (siegeWaveTickCounter >= intervalTicks) {
                siegeWaveTickCounter = 0;
                siegeWaveCounter++;
                spawnControlledSiegeWave(level);
            }
        } else {
            siegeWaveTickCounter = 0;
            siegeWaveCounter = 0;
        }

        // Blood Moon Boss cycle (spawns at midnight 18000, freezes time, scales with players, boss bar)
        BloodMoonBossManager.tick(level);

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
        } else if (!isBloodMoon && !DARKNESS_PULSE_COOLDOWNS.isEmpty()) {
            DARKNESS_PULSE_COOLDOWNS.clear();
        }
    }

    private static void spawnControlledSiegeWave(ServerLevel level) {
        boolean escalation = EnjoyNightsConfig.SERVER.bloodMoonSiegeEscalationEnabled.get();
        int baselineCap = EnjoyNightsConfig.SERVER.bloodMoonMaxNearbyMobs.get();
        int nightCap = Math.max(baselineCap, EnjoyNightsConfig.SERVER.bloodMoonSiegeMaxCapNight.get());
        RandomSource random = level.getRandom();

        long timeOfDay = NightAndMoonHelper.getTimeOfDay(level);
        boolean pastMidnight = timeOfDay >= 18000L;
        int wavesBeforeElite = EnjoyNightsConfig.SERVER.bloodMoonSiegeEscalationWavesBeforeElite.get();
        // Rare escalation units (husk, stray, witch, vindicator, breeze, elite wither
        // skeleton) only appear in late waves: past midnight AND after enough waves.
        boolean lateWave = escalation && pastMidnight && siegeWaveCounter >= wavesBeforeElite;

        // Effective cap grows with escalation as the night deepens, never exceeding the
        // configured nighttime ceiling so performance stays bounded.
        int effectiveCap = escalation ? interpolateCap(baselineCap, nightCap, timeOfDay) : baselineCap;

        int minMobs = EnjoyNightsConfig.SERVER.bloodMoonSiegeWaveMinMobs.get();
        int maxMobs = Math.max(minMobs, EnjoyNightsConfig.SERVER.bloodMoonSiegeWaveMaxMobs.get());

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }

            // Cap verification: count existing hostile monsters within 32 blocks.
            AABB searchBox = player.getBoundingBox().inflate(32.0);
            List<Monster> nearbyMonsters = level.getEntitiesOfClass(Monster.class, searchBox);
            int headroom = effectiveCap - nearbyMonsters.size();
            if (headroom <= 0) {
                continue;
            }

            // Base wave size plus an escalation bonus from elapsed waves (capped), then
            // clamp to the remaining headroom under the effective cap.
            int waveSize = minMobs + random.nextInt(maxMobs - minMobs + 1);
            if (escalation) {
                waveSize += Math.min(siegeWaveCounter / 2, maxMobs);
            }
            int mobsToSpawn = Math.min(waveSize, headroom);

            for (int i = 0; i < mobsToSpawn; i++) {
                double angle = random.nextFloat() * (float) (2 * Math.PI);
                double distance = 20.0 + random.nextDouble() * 8.0;

                int spawnX = Mth.floor(player.getX() + Math.cos(angle) * distance);
                int spawnZ = Mth.floor(player.getZ() + Math.sin(angle) * distance);
                int spawnY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);

                BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);

                // While a boss arena is active, suppress regular siege spawns within the
                // suppression radius (wider than the battle arena) so the surroundings stay
                // clear during the fight. The boss still summons its own minions directly.
                // Spawns resume everywhere once the boss is defeated (anchor cleared).
                if (EnjoyNightsConfig.SERVER.bloodMoonBossArenaSuppressSieges.get()
                        && BloodMoonBossManager.isArenaActive()) {
                    BlockPos anchor = BloodMoonBossManager.getArenaAnchor();
                    int suppressRadius = BloodMoonBossManager.getSuppressRadius();
                    if (anchor != null && anchor.distSqr(spawnPos) <= (double) suppressRadius * suppressRadius) {
                        continue;
                    }
                }

                // If inside a ward, relocate to the perimeter outside the base.
                WardingSavedData.WardEntry ward = WardingSavedData.get(level).findNearestActiveWard(spawnPos, EnjoyNightsConfig.SERVER.wardingTotemRadius.get());
                if (ward != null) {
                    spawnPos = WardingSavedData.calculatePerimeterRelocation(level, spawnPos, ward.pos(), EnjoyNightsConfig.SERVER.wardingTotemRadius.get());
                    spawnX = spawnPos.getX();
                    spawnY = spawnPos.getY();
                    spawnZ = spawnPos.getZ();
                }

                // Ensure reasonable vertical distance from the player and a valid air block.
                if (Math.abs(spawnY - player.getBlockY()) <= 12 && level.getBlockState(spawnPos).isAir()) {
                    EntityType<? extends Mob> type = pickWaveType(random, lateWave);
                    spawnSiegeMob(level, type, spawnX, spawnY, spawnZ, player, random, lateWave);
                }
            }
        }
    }

    /**
     * Linearly interpolates the nearby-mob cap from the baseline at dusk to the full
     * nighttime ceiling at (and past) midnight, so the siege intensifies over the night.
     */
    private static int interpolateCap(int baselineCap, int nightCap, long timeOfDay) {
        long duskStart = NightAndMoonHelper.DUSK_TIME; // 12500
        long midnight = 18000L;
        if (timeOfDay >= midnight) {
            return nightCap;
        }
        if (timeOfDay <= duskStart) {
            return baselineCap;
        }
        double progress = (double) (timeOfDay - duskStart) / (double) (midnight - duskStart);
        return baselineCap + (int) Math.round((nightCap - baselineCap) * progress);
    }

    /**
     * Picks a siege mob type using the configured composition weights. Core units
     * (zombie, cave spider, skeleton) are always eligible; the rare escalation units
     * only contribute their weight during late waves.
     */
    private static EntityType<? extends Mob> pickWaveType(RandomSource random, boolean lateWave) {
        int zombie = EnjoyNightsConfig.SERVER.bloodMoonWaveZombieWeight.get();
        int caveSpider = EnjoyNightsConfig.SERVER.bloodMoonWaveCaveSpiderWeight.get();
        int skeleton = EnjoyNightsConfig.SERVER.bloodMoonWaveSkeletonWeight.get();
        int husk = lateWave ? EnjoyNightsConfig.SERVER.bloodMoonWaveHuskWeight.get() : 0;
        int stray = lateWave ? EnjoyNightsConfig.SERVER.bloodMoonWaveStrayWeight.get() : 0;
        int witch = lateWave ? EnjoyNightsConfig.SERVER.bloodMoonWaveWitchWeight.get() : 0;
        int vindicator = lateWave ? EnjoyNightsConfig.SERVER.bloodMoonWaveVindicatorWeight.get() : 0;
        int breeze = lateWave ? EnjoyNightsConfig.SERVER.bloodMoonWaveBreezeWeight.get() : 0;

        int total = zombie + caveSpider + skeleton + husk + stray + witch + vindicator + breeze;
        if (total <= 0) {
            return EntityType.ZOMBIE; // Safe fallback if all weights are zero.
        }

        int roll = random.nextInt(total);
        if ((roll -= zombie) < 0) return EntityType.ZOMBIE;
        if ((roll -= caveSpider) < 0) return EntityType.CAVE_SPIDER;
        if ((roll -= skeleton) < 0) return EntityType.SKELETON;
        if ((roll -= husk) < 0) return EntityType.HUSK;
        if ((roll -= stray) < 0) return EntityType.STRAY;
        if ((roll -= witch) < 0) return EntityType.WITCH;
        if ((roll -= vindicator) < 0) return EntityType.VINDICATOR;
        return EntityType.BREEZE;
    }

    /**
     * Spawns a single siege mob of the given type, tagging it for cleanup and applying
     * any type-specific markers (cave spider poison tag, elite wither skeleton upgrade).
     */
    private static void spawnSiegeMob(ServerLevel level, EntityType<? extends Mob> type,
                                      int x, int y, int z, ServerPlayer target,
                                      RandomSource random, boolean lateWave) {
        // Rare elite upgrade: a late-wave skeleton slot may instead become an elite
        // Wither Skeleton (applies Wither on hit). Never tagged as the Crimson boss.
        boolean elite = false;
        if (lateWave && type == EntityType.SKELETON
                && random.nextDouble() < EnjoyNightsConfig.SERVER.bloodMoonWitherSkeletonEliteChance.get()) {
            type = EntityType.WITHER_SKELETON;
            elite = true;
        }

        Mob mob = type.create(level, EntitySpawnReason.REINFORCEMENT);
        if (mob == null) {
            return;
        }

        BlockPos spawnPos = new BlockPos(x, y, z);
        mob.setPos(x + 0.5, (double) y, z + 0.5);
        mob.setYRot(random.nextFloat() * 360.0F);
        mob.addTag(LunarPhaseEvents.TAG_EXTRA_SPAWN);

        if (type == EntityType.CAVE_SPIDER) {
            mob.addTag(LunarPhaseEvents.TAG_CAVE_SPIDER);
        } else if (elite) {
            mob.addTag(LunarPhaseEvents.TAG_WITHER_ELITE);
            // Minor durability buff so the elite is a memorable threat.
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, 0, false, true, true));
        }

        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.REINFORCEMENT, null);

        // Siege mobs are spawned manually and never fire FinalizeSpawnEvent, so apply the
        // Blood Moon empowerment (potion buffs, follow range, charged creepers, flame
        // skeletons) directly — otherwise they would spawn as plain, un-powered mobs.
        if (mob instanceof Monster siegeMonster) {
            LunarPhaseEvents.applyLunarEmpowerment(siegeMonster, false, true);
        }

        if (mob instanceof Monster) {
            mob.setTarget(target);
        }
        level.addFreshEntity(mob);
    }

    @SubscribeEvent
    public static void onCheckSpawn(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = event.getEntity().blockPosition();

            // Boss arena: suppress natural spawns inside the suppression radius while a
            // boss is active. Mobs summoned by the boss (tagged extra-spawn / boss) are
            // ALWAYS exempt so its reinforcements still appear even though they fall
            // within the suppression radius — the suppression rule only targets regular
            // natural/siege spawns. (Boss minions are also added via addFreshEntity, which
            // does not fire this check; this tag guard is a defensive double-safety.)
            if (EnjoyNightsConfig.SERVER.bloodMoonBossArenaSuppressSieges.get()
                    && BloodMoonBossManager.isArenaActive()
                    && event.getEntity() instanceof Monster
                    && !event.getEntity().getTags().contains(LunarPhaseEvents.TAG_EXTRA_SPAWN)
                    && !event.getEntity().getTags().contains(BloodMoonBossManager.TAG_BOSS)) {
                BlockPos anchor = BloodMoonBossManager.getArenaAnchor();
                int suppressRadius = BloodMoonBossManager.getSuppressRadius();
                if (anchor != null && anchor.distSqr(pos) <= (double) suppressRadius * suppressRadius) {
                    event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL);
                    return;
                }
            }

            WardingSavedData.WardEntry ward = WardingSavedData.get(serverLevel)
                    .findNearestActiveWard(pos, EnjoyNightsConfig.SERVER.wardingTotemRadius.get());

            if (ward != null) {
                // Relocate mob outside the protection radius!
                BlockPos perimeterPos = WardingSavedData.calculatePerimeterRelocation(
                        serverLevel, pos, ward.pos(), EnjoyNightsConfig.SERVER.wardingTotemRadius.get()
                );
                event.getEntity().setPos(perimeterPos.getX() + 0.5, (double) perimeterPos.getY(), perimeterPos.getZ() + 0.5);
                if (event.getEntity().checkSpawnObstruction(serverLevel)) {
                    event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.SUCCEED);
                } else {
                    event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL);
                }
                return;
            }

            // Outside ward during Blood Moon: ignore torch/light level so monsters can
            // spawn even in lit areas. The vanilla light-level rule is evaluated in this
            // PositionCheck phase, so we deterministically override the result to SUCCEED
            // for monsters (as long as the physical space is valid), rather than leaving
            // it at DEFAULT where the light rule would reject lit positions.
            // Guarded to the Overworld so the Blood Moon never affects the Nether/End.
            if (serverLevel.dimension() == Level.OVERWORLD
                    && NightAndMoonHelper.isBloodMoon(serverLevel)
                    && EnjoyNightsConfig.SERVER.bloodMoonIgnoreTorches.get()
                    && event.getEntity() instanceof Monster monster) {
                if (monster.checkSpawnObstruction(serverLevel)) {
                    // Physical space is clear: force the spawn regardless of light level.
                    event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.SUCCEED);
                } else {
                    // Mob would be stuck inside blocks: reject so we never spawn suffocating mobs.
                    event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL);
                }
            }
        }
    }

    /**
     * Evaluates and dispatches the chat-only anticipatory warnings each tick:
     *   - Blood Moon advance warning, once, exactly N days before a Blood Moon.
     *   - Blood Moon pre-dusk warning, once, in the sunset window of the Blood Moon day.
     *   - Full Moon pre-dusk warning, once, in the sunset window of a Full Moon day.
     *
     * All warnings latch on the absolute day number so they fire at most once per
     * relevant day despite running ~20 times per second. The pre-dusk checks use a
     * tick window (not an exact-tick equality) so a laggy or skipped tick cannot miss
     * the sunset moment. These are chat messages only: no sounds and no cinematic
     * (the Blood Moon cinematic still fires separately when night actually falls).
     */
    private static void processAnticipatoryWarnings(ServerLevel level) {
        long day = NightAndMoonHelper.getDay(level);

        // Reset per-day latches whenever the day number changes (new dawn or time command).
        if (day != lastProcessedDay) {
            lastProcessedDay = day;
            bloodMoonPreDuskWarnedDay = NO_DAY;
            fullMoonPreDuskWarnedDay = NO_DAY;
        }

        boolean siegeEnabled = EnjoyNightsConfig.SERVER.bloodMoonSiegeEnabled.get();

        // 1) Blood Moon advance warning: exactly `daysBefore` days before the next Blood Moon.
        if (siegeEnabled && EnjoyNightsConfig.SERVER.bloodMoonWarningEnabled.get()) {
            int interval = EnjoyNightsConfig.SERVER.bloodMoonIntervalDays.get();
            int daysBefore = EnjoyNightsConfig.SERVER.bloodMoonWarningDaysBefore.get();
            // Skip when the lead would overlap the Blood Moon day itself or a too-short interval.
            if (interval > daysBefore) {
                long daysUntil = NightAndMoonHelper.getDaysUntilNextBloodMoon(level);
                if (daysUntil == daysBefore && bloodMoonDaysWarnedDay != day) {
                    bloodMoonDaysWarnedDay = day;
                    broadcastChat(level, Component.translatable("enjoy_nights.bloodmoon.incoming_days", daysUntil)
                            .withStyle(ChatFormatting.RED));
                }
            }
        }

        // Pre-dusk (sunset) warnings only make sense while it is still daytime.
        if (NightAndMoonHelper.isNight(level)) {
            return;
        }

        // 2) Blood Moon pre-dusk warning on the Blood Moon day. Takes precedence over Full Moon.
        boolean tonightIsBloodMoon = siegeEnabled && NightAndMoonHelper.isBloodMoonDayNumber(level, day);
        if (tonightIsBloodMoon
                && EnjoyNightsConfig.SERVER.bloodMoonPreDuskWarningEnabled.get()
                && NightAndMoonHelper.isInPreDuskWindow(level, EnjoyNightsConfig.SERVER.bloodMoonPreDuskWarningTicks.get())
                && bloodMoonPreDuskWarnedDay != day) {
            bloodMoonPreDuskWarnedDay = day;
            broadcastChat(level, Component.translatable("enjoy_nights.bloodmoon.pre_dusk")
                    .withStyle(ChatFormatting.DARK_RED));
            return;
        }

        // 3) Full Moon pre-dusk warning on a Full Moon day (suppressed if tonight is a Blood Moon).
        boolean tonightIsFullMoon = NightAndMoonHelper.getMoonPhaseForDay(day) == NightAndMoonHelper.MOON_PHASE_FULL;
        if (!tonightIsBloodMoon
                && tonightIsFullMoon
                && EnjoyNightsConfig.SERVER.fullMoonWarningEnabled.get()
                && NightAndMoonHelper.isInPreDuskWindow(level, EnjoyNightsConfig.SERVER.fullMoonPreDuskWarningTicks.get())
                && fullMoonPreDuskWarnedDay != day) {
            fullMoonPreDuskWarnedDay = day;
            broadcastChat(level, Component.translatable("enjoy_nights.fullmoon.pre_dusk")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static void broadcastChat(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
        }
    }

    /**
     * Fires the end-of-event mob sweep once when a Full Moon night ends (transitions
     * to day/next phase). When a night is simultaneously a Blood Moon, the Blood Moon
     * end path owns the sweep, so this avoids a duplicate pass by deferring while a
     * Blood Moon is active.
     */
    private static void processFullMoonEndSweep(ServerLevel level) {
        if (!EnjoyNightsConfig.SERVER.eventMobCleanupEnabled.get()) {
            fullMoonActive = false;
            return;
        }

        boolean isFullMoonNow = NightAndMoonHelper.isFullMoon(level);

        if (isFullMoonNow) {
            fullMoonActive = true;
            return;
        }

        if (fullMoonActive) {
            fullMoonActive = false;
            // If a Blood Moon is still active, let the Blood Moon end path sweep instead.
            if (!NightAndMoonHelper.isBloodMoon(level)) {
                sweepEventMobs(level);
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

        // Full lifecycle sweep: delete event-spawned mobs, revert empowered natural mobs.
        sweepEventMobs(level);
    }

    /**
     * Purges lingering night mobs from the specified level by delegating each tagged
     * mob to {@link com.coderdiaz.enjoynights.util.EventMobCleanup}.
     *
     * @param level ServerLevel to scan
     * @param forceDeletePowered when true, empowered natural mobs are deleted outright
     *                           (used by the admin {@code /enjoynights purge} command);
     *                           when false they are reverted to normal instead
     * @return number of entities removed from the world
     */
    public static int purgeNightMobs(ServerLevel level, boolean forceDeletePowered) {
        boolean bossActive = BloodMoonBossManager.isBossActive();
        int removedCount = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob mob && mob.isAlive()) {
                com.coderdiaz.enjoynights.util.EventMobCleanup.Action action =
                        com.coderdiaz.enjoynights.util.EventMobCleanup.cleanupTaggedMob(mob, bossActive, forceDeletePowered);
                if (action == com.coderdiaz.enjoynights.util.EventMobCleanup.Action.DELETED) {
                    removedCount++;
                }
            }
        }
        return removedCount;
    }

    /**
     * Performs the end-of-event sweep across all loaded chunks of the level: deletes
     * event-spawned mobs and reverts naturally-spawned empowered mobs to normal. Runs
     * as a single pass, intended to fire once when a Full Moon or Blood Moon ends.
     * Mobs in unloaded chunks are handled later by {@link #onEntityJoinLevel} when
     * their chunk reloads.
     */
    public static void sweepEventMobs(ServerLevel level) {
        if (!EnjoyNightsConfig.SERVER.eventMobCleanupEnabled.get()) {
            return;
        }
        boolean bossActive = BloodMoonBossManager.isBossActive();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob mob && mob.isAlive()) {
                com.coderdiaz.enjoynights.util.EventMobCleanup.cleanupTaggedMob(mob, bossActive, false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DARKNESS_PULSE_COOLDOWNS.remove(event.getEntity().getUUID());
    }

    /**
     * The Blood Moon is an Overworld-only event. When a player travels to another
     * dimension (Nether/End) they must stop receiving its effects, and when they
     * return they are naturally re-included by the per-tick {@code level.players()}
     * scan. Here we clear any lingering Blood Moon state (darkness pulse cooldown and
     * the residual Darkness effect) the moment a player leaves the Overworld, so the
     * event does not "follow" them into another dimension.
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean leftOverworld = event.getTo() != Level.OVERWORLD;
        if (leftOverworld) {
            DARKNESS_PULSE_COOLDOWNS.remove(player.getUUID());
            if (player.hasEffect(MobEffects.DARKNESS)) {
                player.removeEffect(MobEffects.DARKNESS);
            }
        }
    }
}
