package com.coderdiaz.enjoynights.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class EnjoyNightsConfig {
    public static class Server {
        public final ModConfigSpec.BooleanValue preventNightSkip;
        public final ModConfigSpec.IntValue bedSleepDurationSeconds;
        public final ModConfigSpec.IntValue bedBuffDurationSeconds;
        public final ModConfigSpec.IntValue bedRestCooldownSeconds;
        public final ModConfigSpec.BooleanValue preventPhantoms;
        public final ModConfigSpec.DoubleValue fullMoonDiamondDropChance;
        public final ModConfigSpec.BooleanValue fullMoonExtraDropsEnabled;
        public final ModConfigSpec.DoubleValue newMoonStealthFactor;
        public final ModConfigSpec.BooleanValue fullMoonMobBuffsEnabled;
        public final ModConfigSpec.IntValue fullMoonExperienceMultiplier;
        public final ModConfigSpec.DoubleValue fullMoonChargedCreeperChance;
        public final ModConfigSpec.DoubleValue bloodMoonChargedCreeperChance;
        public final ModConfigSpec.BooleanValue fullMoonWarningEnabled;
        public final ModConfigSpec.IntValue fullMoonPreDuskWarningTicks;
        public final ModConfigSpec.BooleanValue eventMobCleanupEnabled;
        public final ModConfigSpec.DoubleValue fullMoonWitchChance;
        public final ModConfigSpec.IntValue paranoiaDurationSeconds;
        public final ModConfigSpec.BooleanValue paranoiaWeaknessEnabled;

        // Warding Totem Configurations
        public final ModConfigSpec.IntValue wardingTotemRadius;
        public final ModConfigSpec.IntValue wardingTotemAmethystCost;

        // Blood Moon & Siege Configurations
        public final ModConfigSpec.IntValue bloodMoonIntervalDays;
        public final ModConfigSpec.BooleanValue bloodMoonSiegeEnabled;
        public final ModConfigSpec.BooleanValue bloodMoonZombiesBreakBlocks;
        public final ModConfigSpec.BooleanValue bloodMoonExtraSpawnsEnabled;
        public final ModConfigSpec.DoubleValue bloodMoonExtraNaturalSpawnChance;
        public final ModConfigSpec.BooleanValue bloodMoonSiegeWavesEnabled;
        public final ModConfigSpec.IntValue bloodMoonMaxNearbyMobs;
        public final ModConfigSpec.IntValue bloodMoonWaveIntervalSeconds;
        public final ModConfigSpec.BooleanValue bloodMoonDarknessPulseEnabled;
        public final ModConfigSpec.BooleanValue bloodMoonIgnoreTorches;
        public final ModConfigSpec.DoubleValue bloodMoonFlameSkeletonChance;

        // Blood Moon Anticipatory Warning Configurations
        public final ModConfigSpec.BooleanValue bloodMoonWarningEnabled;
        public final ModConfigSpec.IntValue bloodMoonWarningDaysBefore;
        public final ModConfigSpec.BooleanValue bloodMoonPreDuskWarningEnabled;
        public final ModConfigSpec.IntValue bloodMoonPreDuskWarningTicks;

        // Blood Moon Siege Scale, Escalation & Composition Configurations
        public final ModConfigSpec.IntValue bloodMoonSiegeWaveMinMobs;
        public final ModConfigSpec.IntValue bloodMoonSiegeWaveMaxMobs;
        public final ModConfigSpec.BooleanValue bloodMoonSiegeEscalationEnabled;
        public final ModConfigSpec.IntValue bloodMoonSiegeMaxCapNight;
        public final ModConfigSpec.IntValue bloodMoonSiegeEscalationWavesBeforeElite;
        public final ModConfigSpec.IntValue bloodMoonWaveZombieWeight;
        public final ModConfigSpec.IntValue bloodMoonWaveCaveSpiderWeight;
        public final ModConfigSpec.IntValue bloodMoonWaveSkeletonWeight;
        public final ModConfigSpec.IntValue bloodMoonWaveHuskWeight;
        public final ModConfigSpec.IntValue bloodMoonWaveStrayWeight;
        public final ModConfigSpec.IntValue bloodMoonWaveWitchWeight;
        public final ModConfigSpec.IntValue bloodMoonWaveVindicatorWeight;
        public final ModConfigSpec.IntValue bloodMoonWaveBreezeWeight;
        public final ModConfigSpec.DoubleValue bloodMoonWitherSkeletonEliteChance;

        // Blood Moon Cave Spider (poison ambusher) Configurations
        public final ModConfigSpec.DoubleValue bloodMoonCaveSpiderChance;
        public final ModConfigSpec.IntValue bloodMoonCaveSpiderPoisonSeconds;
        public final ModConfigSpec.IntValue bloodMoonCaveSpiderPoisonAmplifier;

        // Blood Moon Boss Configurations
        public final ModConfigSpec.BooleanValue bloodMoonBossEnabled;
        public final ModConfigSpec.ConfigValue<String> bloodMoonBossName;
        public final ModConfigSpec.DoubleValue bloodMoonBossBaseHealth;
        public final ModConfigSpec.DoubleValue bloodMoonBossHealthPerPlayer;
        public final ModConfigSpec.IntValue bloodMoonBossHealthDiminishAfterPlayers;
        public final ModConfigSpec.DoubleValue bloodMoonBossHealthDiminishFactor;
        public final ModConfigSpec.IntValue bloodMoonBossMaxMinionsPerPhase;
        public final ModConfigSpec.DoubleValue bloodMoonBossBaseDamage;
        public final ModConfigSpec.DoubleValue bloodMoonBossDamagePerPlayer;
        public final ModConfigSpec.DoubleValue bloodMoonBossSpeed;
        public final ModConfigSpec.DoubleValue bloodMoonBossKnockbackResistance;
        public final ModConfigSpec.IntValue bloodMoonBossExperience;
        public final ModConfigSpec.BooleanValue bloodMoonBossFreezeTime;
        public final ModConfigSpec.IntValue bloodMoonBossFreezeDurationSeconds;
        public final ModConfigSpec.BooleanValue bloodMoonBossDespawnAtDawn;
        public final ModConfigSpec.BooleanValue bloodMoonBossRequireRitual;
        public final ModConfigSpec.DoubleValue bloodMoonBossScalingRadius;
        public final ModConfigSpec.BooleanValue bloodMoonBossSummonMinions;
        public final ModConfigSpec.IntValue bloodMoonBossRoarCooldownSeconds;
        public final ModConfigSpec.DoubleValue bloodMoonBossScale;
        public final ModConfigSpec.BooleanValue bloodMoonBossCleaveEnabled;
        public final ModConfigSpec.DoubleValue bloodMoonBossCleaveAngle;
        public final ModConfigSpec.DoubleValue bloodMoonBossCleaveRadius;
        public final ModConfigSpec.BooleanValue bloodMoonBossLeapAttackEnabled;
        public final ModConfigSpec.IntValue bloodMoonBossLeapCooldownSeconds;
        public final ModConfigSpec.DoubleValue bloodMoonBossLeapRadius;
        public final ModConfigSpec.DoubleValue bloodMoonBossLeapKnockback;
        public final ModConfigSpec.IntValue bloodMoonBossShockwaveCooldownSeconds;
        public final ModConfigSpec.BooleanValue bloodMoonBossChargedAttackEnabled;
        public final ModConfigSpec.IntValue bloodMoonBossChargedAttackCooldownSeconds;
        public final ModConfigSpec.IntValue bloodMoonBossRecoveryTicks;
        public final ModConfigSpec.BooleanValue bloodMoonBossPhase2Enabled;
        public final ModConfigSpec.DoubleValue bloodMoonBossKnockbackMultiplier;
        public final ModConfigSpec.DoubleValue bloodMoonBossMeleeBias;
        public final ModConfigSpec.IntValue bloodMoonBossAbilityLockoutTicks;
        public final ModConfigSpec.DoubleValue bloodMoonBossWitherOnHitChance;
        public final ModConfigSpec.BooleanValue bloodMoonBossStompEnabled;
        public final ModConfigSpec.IntValue bloodMoonBossStompCooldownSeconds;
        public final ModConfigSpec.BooleanValue bloodMoonBossSkullVolleyEnabled;
        public final ModConfigSpec.IntValue bloodMoonBossSkullVolleyCooldownSeconds;
        public final ModConfigSpec.BooleanValue bloodMoonBossSoulPillarsEnabled;
        public final ModConfigSpec.IntValue bloodMoonBossSoulPillarsCooldownSeconds;
        public final ModConfigSpec.IntValue bloodMoonBossArenaRadius;
        public final ModConfigSpec.IntValue bloodMoonBossArenaSuppressRadius;
        public final ModConfigSpec.BooleanValue bloodMoonBossArenaSuppressSieges;
        public final ModConfigSpec.DoubleValue bloodMoonBossArenaRegenPerSecond;

        public Server(ModConfigSpec.Builder builder) {
            builder.comment("Server-side configurations for Enjoy Nights!").push("server");

            builder.push("bed_and_sleep");
            preventNightSkip = builder
                    .comment("Prevent sleeping in beds from skipping the night to morning")
                    .define("preventNightSkip", true);

            bedSleepDurationSeconds = builder
                    .comment("Duration in seconds that a player stays in bed before automatically waking up")
                    .defineInRange("bedSleepDurationSeconds", 10, 3, 60);

            bedBuffDurationSeconds = builder
                    .comment("Duration in seconds of the recovery and stamina buffs granted after sleeping in bed (default: 90s)")
                    .defineInRange("bedBuffDurationSeconds", 90, 10, 600);

            bedRestCooldownSeconds = builder
                    .comment("Cooldown in seconds before a player can rest in bed again to receive buffs (default: 180s = 3 minutes)")
                    .defineInRange("bedRestCooldownSeconds", 180, 10, 1200);
            builder.pop();

            builder.push("phantoms");
            preventPhantoms = builder
                    .comment("Prevent phantoms/nightmares from spawning when the player doesn't sleep")
                    .define("preventPhantoms", true);
            builder.pop();

            builder.push("lunar_phases");
            fullMoonDiamondDropChance = builder
                    .comment("Chance (0.0 to 1.0) for standard monsters to drop a diamond under a Full Moon (default: 0.01 = 1%)")
                    .defineInRange("fullMoonDiamondDropChance", 0.01, 0.0, 1.0);

            fullMoonExtraDropsEnabled = builder
                    .comment("Whether monsters drop additional mineral fragments (iron, gems) under a Full Moon")
                    .define("fullMoonExtraDropsEnabled", true);

            newMoonStealthFactor = builder
                    .comment("Visibility modifier for players during a New Moon night (lower = mobs detect you from closer, default: 0.40 = 60% stealth)")
                    .defineInRange("newMoonStealthFactor", 0.40, 0.1, 1.0);

            fullMoonMobBuffsEnabled = builder
                    .comment("Whether monsters have a chance to spawn with buffs (Strength, Speed) under a Full Moon")
                    .define("fullMoonMobBuffsEnabled", true);

            fullMoonExperienceMultiplier = builder
                    .comment("Experience multiplier when defeating powered monsters under a Full Moon or Blood Moon")
                    .defineInRange("fullMoonExperienceMultiplier", 3, 1, 10);

            fullMoonChargedCreeperChance = builder
                    .comment("Chance (0.0 to 1.0) for creepers to spawn as Charged Creepers during a Full Moon (default: 0.05 = 5%)")
                    .defineInRange("fullMoonChargedCreeperChance", 0.05, 0.0, 1.0);

            bloodMoonChargedCreeperChance = builder
                    .comment("Chance (0.0 to 1.0) for creepers to spawn as Charged Creepers during a Blood Moon (default: 0.25 = 25%)")
                    .defineInRange("bloodMoonChargedCreeperChance", 0.25, 0.0, 1.0);

            fullMoonWarningEnabled = builder
                    .comment("Whether a chat warning is sent shortly before nightfall on a Full Moon day so players can anticipate the event")
                    .define("fullMoonWarningEnabled", true);

            fullMoonPreDuskWarningTicks = builder
                    .comment("Ticks before dusk (tick 12500) to send the Full Moon chat warning on a Full Moon day (default: 200 ticks = 10s; 20 ticks = 1 second)")
                    .defineInRange("fullMoonPreDuskWarningTicks", 200, 20, 2000);

            eventMobCleanupEnabled = builder
                    .comment("Whether mobs spawned or empowered by Full Moon / Blood Moon events are cleaned up when the event ends (event-spawned mobs are removed; naturally-spawned empowered mobs are reverted to normal). Also cleans lingering/legacy event mobs when their chunk reloads outside an event. Prevents entity accumulation and performance loss")
                    .define("eventMobCleanupEnabled", true);

            fullMoonWitchChance = builder
                    .comment("Chance (0.0 to 1.0) for a natural Full Moon monster spawn to be accompanied by a rare, eerie standalone Witch (default: 0.03 = 3%). Full Moon has no sieges; this is a lone atmospheric threat")
                    .defineInRange("fullMoonWitchChance", 0.03, 0.0, 1.0);
            builder.pop();

            builder.push("paranoia");
            paranoiaDurationSeconds = builder
                    .comment("Seconds standing still in a dark closed area at night before paranoia sets in (default: 180s = 3 minutes)")
                    .defineInRange("paranoiaDurationSeconds", 180, 20, 1200);

            paranoiaWeaknessEnabled = builder
                    .comment("Whether paranoia causes weakness and hallucination effects and disturbing sounds")
                    .define("paranoiaWeaknessEnabled", true);
            builder.pop();

            builder.push("warding_totem");
            wardingTotemRadius = builder
                    .comment("Radius in blocks protected by an active Warding Totem from mob spawns (default: 100 blocks)")
                    .defineInRange("wardingTotemRadius", 100, 10, 500);

            wardingTotemAmethystCost = builder
                    .comment("Number of amethyst shards required to activate a Warding Totem (default: 8 shards)")
                    .defineInRange("wardingTotemAmethystCost", 8, 1, 64);
            builder.pop();

            builder.push("blood_moon");
            bloodMoonIntervalDays = builder
                    .comment("Frequency: Number of days between each Blood Moon siege night (default: 10, configurable per server)")
                    .defineInRange("bloodMoonIntervalDays", 10, 1, 365);

            bloodMoonSiegeEnabled = builder
                    .comment("Enable the Blood Moon siege night mechanic")
                    .define("bloodMoonSiegeEnabled", true);

            bloodMoonZombiesBreakBlocks = builder
                    .comment("Whether zombies can break wooden and dirt/dust blocks to breach structures during a Blood Moon")
                    .define("bloodMoonZombiesBreakBlocks", true);

            bloodMoonExtraSpawnsEnabled = builder
                    .comment("Whether additional monsters spawn during Blood Moon nights")
                    .define("bloodMoonExtraSpawnsEnabled", true);

            bloodMoonExtraNaturalSpawnChance = builder
                    .comment("Chance (0.0 to 1.0) for a natural monster spawn during Blood Moon to spawn an additional companion mob (default: 0.35 = 35%)")
                    .defineInRange("bloodMoonExtraNaturalSpawnChance", 0.35, 0.0, 1.0);

            bloodMoonSiegeWavesEnabled = builder
                    .comment("Whether small controlled siege waves spawn periodically near active players during Blood Moon")
                    .define("bloodMoonSiegeWavesEnabled", true);

            bloodMoonMaxNearbyMobs = builder
                    .comment("Maximum number of hostile mobs allowed within 32 blocks of a player before extra Blood Moon spawns are paused (prevents excessive mob swarms)")
                    .defineInRange("bloodMoonMaxNearbyMobs", 16, 4, 60);

            bloodMoonWaveIntervalSeconds = builder
                    .comment("Seconds between siege wave checks around players during Blood Moon (default: 30s)")
                    .defineInRange("bloodMoonWaveIntervalSeconds", 30, 10, 300);

            bloodMoonDarknessPulseEnabled = builder
                    .comment("Whether players outdoors during a Blood Moon experience sudden sporadic 1.75-second darkness pulses and Warden heartbeats every 2-3 minutes to unnerve the player")
                    .define("bloodMoonDarknessPulseEnabled", true);

            bloodMoonIgnoreTorches = builder
                    .comment("Whether monsters during a Blood Moon can spawn regardless of light levels emitted by torches and lanterns outside protected wards (default: true)")
                    .define("bloodMoonIgnoreTorches", true);

            bloodMoonFlameSkeletonChance = builder
                    .comment("Probability (0.0 to 1.0) for skeletons to spawn as Flame Skeletons shooting fire arrows during Blood Moon (default: 0.15 = 15%)")
                    .defineInRange("bloodMoonFlameSkeletonChance", 0.15, 0.0, 1.0);

            bloodMoonWarningEnabled = builder
                    .comment("Whether a chat warning is broadcast a configurable number of days before a Blood Moon siege so players can prepare")
                    .define("bloodMoonWarningEnabled", true);

            bloodMoonWarningDaysBefore = builder
                    .comment("Number of days before a Blood Moon to broadcast the advance chat warning (default: 3). Ignored if it is greater than or equal to bloodMoonIntervalDays")
                    .defineInRange("bloodMoonWarningDaysBefore", 3, 1, 30);

            bloodMoonPreDuskWarningEnabled = builder
                    .comment("Whether a chat warning is sent shortly before nightfall on the Blood Moon day itself (in addition to the cinematic shown when night falls)")
                    .define("bloodMoonPreDuskWarningEnabled", true);

            bloodMoonPreDuskWarningTicks = builder
                    .comment("Ticks before dusk (tick 12500) to send the Blood Moon pre-nightfall chat warning (default: 300 ticks = 15s; 20 ticks = 1 second)")
                    .defineInRange("bloodMoonPreDuskWarningTicks", 300, 20, 2000);

            bloodMoonSiegeWaveMinMobs = builder
                    .comment("Minimum number of mobs spawned per player in a single Blood Moon siege wave (default: 3)")
                    .defineInRange("bloodMoonSiegeWaveMinMobs", 3, 1, 20);

            bloodMoonSiegeWaveMaxMobs = builder
                    .comment("Maximum number of mobs spawned per player in a single Blood Moon siege wave before escalation (default: 5)")
                    .defineInRange("bloodMoonSiegeWaveMaxMobs", 5, 1, 30);

            bloodMoonSiegeEscalationEnabled = builder
                    .comment("Whether siege waves grow larger and unlock rare units as the Blood Moon night progresses (default: true)")
                    .define("bloodMoonSiegeEscalationEnabled", true);

            bloodMoonSiegeMaxCapNight = builder
                    .comment("Maximum hostile mobs within 32 blocks of a player during a Blood Moon siege at full escalation (anti-lag ceiling; should be >= bloodMoonMaxNearbyMobs, default: 24)")
                    .defineInRange("bloodMoonSiegeMaxCapNight", 24, 4, 80);

            bloodMoonSiegeEscalationWavesBeforeElite = builder
                    .comment("Number of siege waves that must pass (and it must be past midnight, tick 18000) before rare escalation units (husk, stray, witch, vindicator, breeze, elite wither skeleton) can appear (default: 3)")
                    .defineInRange("bloodMoonSiegeEscalationWavesBeforeElite", 3, 0, 20);

            bloodMoonWaveZombieWeight = builder
                    .comment("Relative spawn weight of Zombies in siege waves (core unit, default: 30)")
                    .defineInRange("bloodMoonWaveZombieWeight", 30, 0, 100);

            bloodMoonWaveCaveSpiderWeight = builder
                    .comment("Relative spawn weight of poisonous Cave Spiders in siege waves (core unit, default: 15)")
                    .defineInRange("bloodMoonWaveCaveSpiderWeight", 15, 0, 100);

            bloodMoonWaveSkeletonWeight = builder
                    .comment("Relative spawn weight of Skeletons in siege waves (core unit, default: 15)")
                    .defineInRange("bloodMoonWaveSkeletonWeight", 15, 0, 100);

            bloodMoonWaveHuskWeight = builder
                    .comment("Relative spawn weight of Husks in siege waves (rare escalation unit, applies Hunger, default: 8)")
                    .defineInRange("bloodMoonWaveHuskWeight", 8, 0, 100);

            bloodMoonWaveStrayWeight = builder
                    .comment("Relative spawn weight of Strays in siege waves (rare escalation unit, arrows apply Slowness, default: 8)")
                    .defineInRange("bloodMoonWaveStrayWeight", 8, 0, 100);

            bloodMoonWaveWitchWeight = builder
                    .comment("Relative spawn weight of Witches in siege waves (rare escalation support unit, throws debuff potions, default: 4)")
                    .defineInRange("bloodMoonWaveWitchWeight", 4, 0, 100);

            bloodMoonWaveVindicatorWeight = builder
                    .comment("Relative spawn weight of Vindicators in siege waves (rare escalation shock troop, high melee damage, default: 3)")
                    .defineInRange("bloodMoonWaveVindicatorWeight", 3, 0, 100);

            bloodMoonWaveBreezeWeight = builder
                    .comment("Relative spawn weight of Breezes in siege waves (rare escalation anti-fortification unit, fires wind charges, default: 2)")
                    .defineInRange("bloodMoonWaveBreezeWeight", 2, 0, 100);

            bloodMoonWitherSkeletonEliteChance = builder
                    .comment("Chance (0.0 to 1.0) for a single late-wave siege slot to spawn an elite Wither Skeleton (applies Wither on hit, gains Resistance; NOT the Crimson boss) (default: 0.01 = 1%)")
                    .defineInRange("bloodMoonWitherSkeletonEliteChance", 0.01, 0.0, 1.0);

            bloodMoonCaveSpiderChance = builder
                    .comment("Chance (0.0 to 1.0) for a natural Blood Moon monster spawn to be accompanied by a poisonous Cave Spider ambusher (default: 0.20 = 20%)")
                    .defineInRange("bloodMoonCaveSpiderChance", 0.20, 0.0, 1.0);

            bloodMoonCaveSpiderPoisonSeconds = builder
                    .comment("Duration in seconds of the Poison effect applied when a Blood Moon Cave Spider hits a player (default: 5s)")
                    .defineInRange("bloodMoonCaveSpiderPoisonSeconds", 5, 1, 60);

            bloodMoonCaveSpiderPoisonAmplifier = builder
                    .comment("Amplifier of the Poison effect applied by Blood Moon Cave Spiders (0 = Poison I, 1 = Poison II, default: 0)")
                    .defineInRange("bloodMoonCaveSpiderPoisonAmplifier", 0, 0, 4);
            builder.pop();

            builder.push("blood_moon_boss");
            bloodMoonBossEnabled = builder
                    .comment("Whether a powerful boss spawns at midnight (tick 18000) during a Blood Moon (default: true)")
                    .define("bloodMoonBossEnabled", true);

            bloodMoonBossName = builder
                    .comment("Display name of the Blood Moon Boss shown on the boss bar (default: 'Heraldo de la Luna de Sangre')")
                    .define("bloodMoonBossName", "Heraldo de la Luna de Sangre");

            bloodMoonBossBaseHealth = builder
                    .comment("Base maximum health of the Blood Moon Boss for a single player (default: 200.0 = 100 hearts)")
                    .defineInRange("bloodMoonBossBaseHealth", 200.0, 20.0, 5000.0);

            bloodMoonBossHealthPerPlayer = builder
                    .comment("Additional health added to the boss per extra nearby player in 1000 blocks (default: 60.0 = 30 hearts)")
                    .defineInRange("bloodMoonBossHealthPerPlayer", 60.0, 0.0, 2000.0);

            bloodMoonBossHealthDiminishAfterPlayers = builder
                    .comment("Number of extra players after which the per-player health bonus is reduced (diminishing returns). Extra players beyond this threshold add health scaled by bloodMoonBossHealthDiminishFactor, so groups do not face an impossible health wall (default: 1)")
                    .defineInRange("bloodMoonBossHealthDiminishAfterPlayers", 1, 1, 50);

            bloodMoonBossHealthDiminishFactor = builder
                    .comment("Multiplier applied to the per-player health bonus for players beyond bloodMoonBossHealthDiminishAfterPlayers (0.0 to 1.0, default: 0.6 = 60% of normal)")
                    .defineInRange("bloodMoonBossHealthDiminishFactor", 0.6, 0.0, 1.0);

            bloodMoonBossMaxMinionsPerPhase = builder
                    .comment("Maximum reinforcement minions the boss summons per health phase (75%/50%/25%), capping group scaling to keep fights winnable (default: 4)")
                    .defineInRange("bloodMoonBossMaxMinionsPerPhase", 4, 0, 12);

            bloodMoonBossBaseDamage = builder
                    .comment("Base total attack damage of the Blood Moon Boss (default: 11.0, balanced to stay threatening against unenchanted Diamond/Netherite without being punishing)")
                    .defineInRange("bloodMoonBossBaseDamage", 11.0, 1.0, 100.0);

            bloodMoonBossDamagePerPlayer = builder
                    .comment("Additional attack damage per extra nearby player (default: 0.0, boss damage remains fair regardless of player count)")
                    .defineInRange("bloodMoonBossDamagePerPlayer", 0.0, 0.0, 20.0);

            bloodMoonBossSpeed = builder
                    .comment("Movement speed attribute of the Blood Moon Boss (default: 0.20, heavy colossus pace)")
                    .defineInRange("bloodMoonBossSpeed", 0.20, 0.10, 1.0);

            bloodMoonBossKnockbackResistance = builder
                    .comment("Knockback resistance of the Blood Moon Boss from 0.0 to 1.0 (default: 0.90, anti-stunlock)")
                    .defineInRange("bloodMoonBossKnockbackResistance", 0.90, 0.0, 1.0);

            bloodMoonBossExperience = builder
                    .comment("Experience points dropped upon defeating the Blood Moon Boss (default: 500)")
                    .defineInRange("bloodMoonBossExperience", 500, 0, 10000);

            bloodMoonBossFreezeTime = builder
                    .comment("Whether time freezes at midnight (tick 18000) during the Blood Moon Boss battle (default: true)")
                    .define("bloodMoonBossFreezeTime", true);

            bloodMoonBossFreezeDurationSeconds = builder
                    .comment("Maximum duration in seconds that time remains frozen at midnight before resuming natural flow towards dawn (default: 600s = 10 minutes)")
                    .defineInRange("bloodMoonBossFreezeDurationSeconds", 600, 60, 3600);

            bloodMoonBossDespawnAtDawn = builder
                    .comment("Whether the boss despawns in a burst of darkness without rewards if players fail to defeat it before dawn (default: true)")
                    .define("bloodMoonBossDespawnAtDawn", true);

            bloodMoonBossRequireRitual = builder
                    .comment("Whether the boss must be deliberately summoned via the Crimson Altar ritual rather than spawning automatically at midnight (default: true)")
                    .define("bloodMoonBossRequireRitual", true);

            bloodMoonBossScalingRadius = builder
                    .comment("Radius in blocks around the Crimson Altar used to count players for boss health and damage scaling (default: 1000.0 blocks)")
                    .defineInRange("bloodMoonBossScalingRadius", 1000.0, 50.0, 10000.0);

            bloodMoonBossSummonMinions = builder
                    .comment("Whether the boss periodically summons reinforcement minions at 75%, 50%, and 25% health (default: true)")
                    .define("bloodMoonBossSummonMinions", true);

            bloodMoonBossRoarCooldownSeconds = builder
                    .comment("Cooldown in seconds between atmospheric roar attacks that inflict Darkness and Slowness (default: 30s)")
                    .defineInRange("bloodMoonBossRoarCooldownSeconds", 30, 5, 300);

            bloodMoonBossScale = builder
                    .comment("Visual and hitbox scale multiplier of the Blood Moon Boss (default: 1.7)")
                    .defineInRange("bloodMoonBossScale", 1.7, 1.0, 3.0);

            bloodMoonBossCleaveEnabled = builder
                    .comment("Whether the boss deals cleaving frontal cone damage hitting multiple players (default: true)")
                    .define("bloodMoonBossCleaveEnabled", true);

            bloodMoonBossCleaveAngle = builder
                    .comment("Frontal cone angle in degrees for cleaving attacks (default: 140.0)")
                    .defineInRange("bloodMoonBossCleaveAngle", 140.0, 45.0, 360.0);

            bloodMoonBossCleaveRadius = builder
                    .comment("Maximum reach radius in blocks for cleaving attacks (default: 4.0)")
                    .defineInRange("bloodMoonBossCleaveRadius", 4.0, 2.0, 10.0);

            bloodMoonBossLeapAttackEnabled = builder
                    .comment("Whether the boss executes a telegraphed devastating leap slam at distant players/archers (default: true)")
                    .define("bloodMoonBossLeapAttackEnabled", true);

            bloodMoonBossLeapCooldownSeconds = builder
                    .comment("Cooldown in seconds between leap slam attacks (default: 14s)")
                    .defineInRange("bloodMoonBossLeapCooldownSeconds", 14, 5, 60);

            bloodMoonBossLeapRadius = builder
                    .comment("Radius in blocks of the shockwave generated when the boss lands its leap slam, pushing players away to force the group to disperse (default: 6.0)")
                    .defineInRange("bloodMoonBossLeapRadius", 6.0, 3.0, 16.0);

            bloodMoonBossLeapKnockback = builder
                    .comment("Strength of the outward knockback applied to players by the leap slam landing shockwave (default: 1.6)")
                    .defineInRange("bloodMoonBossLeapKnockback", 1.6, 0.5, 4.0);

            bloodMoonBossShockwaveCooldownSeconds = builder
                    .comment("Cooldown in seconds between ground fissure/shockwave attacks that knock players into the air (default: 18s)")
                    .defineInRange("bloodMoonBossShockwaveCooldownSeconds", 18, 5, 60);

            bloodMoonBossChargedAttackEnabled = builder
                    .comment("Whether the boss performs a telegraphed charged lunge and wide sweeping attack with dynamic target tracking (default: true)")
                    .define("bloodMoonBossChargedAttackEnabled", true);

            bloodMoonBossChargedAttackCooldownSeconds = builder
                    .comment("Cooldown in seconds between charged lunge sweep attacks (default: 12s)")
                    .defineInRange("bloodMoonBossChargedAttackCooldownSeconds", 12, 5, 60);

            bloodMoonBossRecoveryTicks = builder
                    .comment("Recovery pause duration in ticks (punish window) after executing heavy moves like leap, shockwave or charged lunge (default: 35 ticks = ~1.75s)")
                    .defineInRange("bloodMoonBossRecoveryTicks", 35, 0, 80);

            bloodMoonBossPhase2Enabled = builder
                    .comment("Whether the boss enters Phase 2 'Furia del Eclipse' when below 40% health (default: true)")
                    .define("bloodMoonBossPhase2Enabled", true);

            bloodMoonBossKnockbackMultiplier = builder
                    .comment("Global multiplier applied to the knockback the boss inflicts on players with its heavy moves (leap, shockwave, charged lunge, repulsion). Lower it to keep players more grounded; 1.0 = default strength (default: 1.0)")
                    .defineInRange("bloodMoonBossKnockbackMultiplier", 1.0, 0.0, 2.0);

            bloodMoonBossMeleeBias = builder
                    .comment("Probability (0.0 to 1.0) that the boss throws an ordinary melee strike instead of a heavy move when a player is within melee reach. Higher values favor readable basic attacks over constant lunges (default: 0.5)")
                    .defineInRange("bloodMoonBossMeleeBias", 0.5, 0.0, 1.0);

            bloodMoonBossAbilityLockoutTicks = builder
                    .comment("Shared lockout in ticks between the boss's heavy moves (leap/shockwave/charged lunge), applied in both phases so heavy moves never chain back-to-back (default: 65 ticks = ~3.25s; 20 ticks = 1s)")
                    .defineInRange("bloodMoonBossAbilityLockoutTicks", 65, 20, 200);

            bloodMoonBossWitherOnHitChance = builder
                    .comment("Probability (0.0 to 1.0) that a melee hit from the boss inflicts Wither on the target. 0 disables it (default: 0.25 = 25%)")
                    .defineInRange("bloodMoonBossWitherOnHitChance", 0.25, 0.0, 1.0);

            bloodMoonBossStompEnabled = builder
                    .comment("Enable the colossus Stomp: a short-range ground slam that damages and briefly slows players near the boss's feet, conveying its weight (default: true)")
                    .define("bloodMoonBossStompEnabled", true);

            bloodMoonBossStompCooldownSeconds = builder
                    .comment("Cooldown in seconds between Stomp attacks (default: 10s)")
                    .defineInRange("bloodMoonBossStompCooldownSeconds", 10, 3, 60);

            bloodMoonBossSkullVolleyEnabled = builder
                    .comment("Enable the Wither Skull volley: a dodgeable ranged attack where the boss launches homing wither skulls at mid/long range (default: true)")
                    .define("bloodMoonBossSkullVolleyEnabled", true);

            bloodMoonBossSkullVolleyCooldownSeconds = builder
                    .comment("Cooldown in seconds between Wither Skull volleys (default: 11s)")
                    .defineInRange("bloodMoonBossSkullVolleyCooldownSeconds", 11, 4, 60);

            bloodMoonBossSoulPillarsEnabled = builder
                    .comment("Enable Soul Fire Pillars: telegraphed danger zones that erupt under players' positions after a brief warning, forcing repositioning (default: true)")
                    .define("bloodMoonBossSoulPillarsEnabled", true);

            bloodMoonBossSoulPillarsCooldownSeconds = builder
                    .comment("Cooldown in seconds between Soul Fire Pillar casts (default: 16s)")
                    .defineInRange("bloodMoonBossSoulPillarsCooldownSeconds", 16, 5, 90);

            bloodMoonBossArenaRadius = builder
                    .comment("Radius in blocks of the boss battle arena anchored at the Crimson Altar. The boss cannot leave this radius and retargets players inside it (default: 56)")
                    .defineInRange("bloodMoonBossArenaRadius", 56, 24, 128);

            bloodMoonBossArenaSuppressRadius = builder
                    .comment("Radius in blocks around the altar where regular Blood Moon siege spawns are suppressed while the boss is active. Typically larger than the battle arena so the surroundings stay clear during the fight; spawns resume everywhere once the boss is defeated (default: 72)")
                    .defineInRange("bloodMoonBossArenaSuppressRadius", 72, 24, 256);

            bloodMoonBossArenaSuppressSieges = builder
                    .comment("Suppress regular Blood Moon siege spawns within the suppression radius while the boss is active (the boss still summons its own minions) (default: true)")
                    .define("bloodMoonBossArenaSuppressSieges", true);

            bloodMoonBossArenaRegenPerSecond = builder
                    .comment("Health per second the boss regenerates while no player remains inside the arena and it has returned to the altar (reset-style penalty for fleeing; 0 disables) (default: 2.0)")
                    .defineInRange("bloodMoonBossArenaRegenPerSecond", 2.0, 0.0, 20.0);
            builder.pop();

            builder.pop();
        }
    }

    public static class Client {
        public final ModConfigSpec.BooleanValue bloodMoonFogTint;
        public final ModConfigSpec.BooleanValue bloodMoonScreenVignette;
        public final ModConfigSpec.DoubleValue bloodMoonVignetteIntensity;
        public final ModConfigSpec.BooleanValue bloodMoonComplementarySpecific;
        public final ModConfigSpec.BooleanValue bloodMoonParticles;
        public final ModConfigSpec.BooleanValue paranoiaSoundsEnabled;
        public final ModConfigSpec.BooleanValue bloodMoonPhantomFootstepsEnabled;
        public final ModConfigSpec.DoubleValue bloodMoonShaderAtmosphereIntensity;

        public Client(ModConfigSpec.Builder builder) {
            builder.comment("Client-side configurations for Enjoy Nights!").push("client");

            bloodMoonFogTint = builder
                    .comment("Tint the vanilla atmosphere and fog blood-red during a Blood Moon night")
                    .define("bloodMoonFogTint", true);

            bloodMoonScreenVignette = builder
                    .comment("Display a smooth, circular dark-crimson vignette overlay on screen during Blood Moon")
                    .define("bloodMoonScreenVignette", true);

            bloodMoonVignetteIntensity = builder
                    .comment("Intensity of the blood-red vignette on screen edges during Blood Moon (0.1 to 1.0, default: 0.70)")
                    .defineInRange("bloodMoonVignetteIntensity", 0.70, 0.1, 1.0);

            bloodMoonComplementarySpecific = builder
                    .comment("Whether to automatically apply an optimized softer vignette balance when Complementary Unbound shader is detected")
                    .define("bloodMoonComplementarySpecific", true);

            bloodMoonParticles = builder
                    .comment("Spawn ambient crimson ember spores floating around the player during Blood Moon nights")
                    .define("bloodMoonParticles", true);

            paranoiaSoundsEnabled = builder
                    .comment("Play disturbing hallucinations and ambient sounds when paranoia is triggered")
                    .define("paranoiaSoundsEnabled", true);

            bloodMoonPhantomFootstepsEnabled = builder
                    .comment("Whether players experience phantom footsteps rushing behind them when they stop or mine during a Blood Moon")
                    .define("bloodMoonPhantomFootstepsEnabled", true);

            bloodMoonShaderAtmosphereIntensity = builder
                    .comment("Intensity of the red atmospheric sky & cloud tint passed to shaders during Blood Moon (0.0 to 1.0, default: 0.35)")
                    .defineInRange("bloodMoonShaderAtmosphereIntensity", 0.35, 0.0, 1.0);

            builder.pop();
        }
    }

    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Server, ModConfigSpec> serverPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();

        Pair<Client, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }
}
