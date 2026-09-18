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
        public final ModConfigSpec.IntValue paranoiaDurationSeconds;
        public final ModConfigSpec.BooleanValue paranoiaWeaknessEnabled;

        // Blood Moon & Siege Configurations
        public final ModConfigSpec.IntValue bloodMoonIntervalDays;
        public final ModConfigSpec.BooleanValue bloodMoonSiegeEnabled;
        public final ModConfigSpec.BooleanValue bloodMoonZombiesBreakBlocks;
        public final ModConfigSpec.BooleanValue bloodMoonExtraSpawnsEnabled;
        public final ModConfigSpec.DoubleValue bloodMoonExtraNaturalSpawnChance;
        public final ModConfigSpec.BooleanValue bloodMoonSiegeWavesEnabled;
        public final ModConfigSpec.IntValue bloodMoonMaxNearbyMobs;
        public final ModConfigSpec.IntValue bloodMoonWaveIntervalSeconds;

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
            builder.pop();

            builder.push("paranoia");
            paranoiaDurationSeconds = builder
                    .comment("Seconds standing still in a dark closed area at night before paranoia sets in (default: 180s = 3 minutes)")
                    .defineInRange("paranoiaDurationSeconds", 180, 20, 1200);

            paranoiaWeaknessEnabled = builder
                    .comment("Whether paranoia causes weakness and hallucination effects and disturbing sounds")
                    .define("paranoiaWeaknessEnabled", true);
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
