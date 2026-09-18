package com.coderdiaz.enjoynights.client;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public class ClientEvents {

    private static final net.minecraft.resources.Identifier VIGNETTE_LOCATION =
            net.minecraft.resources.Identifier.withDefaultNamespace("textures/misc/vignette.png");
    private static final net.minecraft.resources.Identifier BLOOD_VIGNETTE_LOCATION =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("enjoy_nights", "textures/gui/blood_vignette.png");

    private static int ambientSoundCooldown = 160;

    private static double lastPlayerSpeedSqr = 0.0;
    private static int phantomFootstepsCooldown = 60;
    private static int phantomStepsRemaining = 0;
    private static int phantomStepDelay = 0;
    private static Vec3 phantomStepPos = null;
    private static SoundEvent phantomStepSound = null;

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null) {
            return;
        }

        // Blood Moon balanced horizon fog effect in vanilla
        if (EnjoyNightsConfig.CLIENT.bloodMoonFogTint.get() && NightAndMoonHelper.isBloodMoon(level)) {
            // Subtle, balanced crimson horizon tint in vanilla (not neon, preserves visibility)
            float baseRed = event.getRed();
            float red = Math.min(0.62f, baseRed * 1.25f + 0.20f);
            float green = event.getGreen() * 0.40f;
            float blue = event.getBlue() * 0.40f;

            event.setRed(red);
            event.setGreen(green);
            event.setBlue(blue);
            return;
        }

        // New Moon pitch-dark fog effect
        if (NightAndMoonHelper.isNewMoon(level)) {
            event.setRed(event.getRed() * 0.5f);
            event.setGreen(event.getGreen() * 0.5f);
            event.setBlue(event.getBlue() * 0.5f);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null) {
            return;
        }

        // Balanced horizon fog for Blood Moon in vanilla:
        // Do NOT choke near distance. Keep near far enough (28+ blocks) so the player's
        // immediate surroundings and landscape are clearly visible, while the horizon has an eerie red haze.
        if (EnjoyNightsConfig.CLIENT.bloodMoonFogTint.get() && NightAndMoonHelper.isBloodMoon(level)) {
            if (!ShaderDetectionHelper.isShaderPackInUse()) {
                float far = event.getFarPlaneDistance();
                event.setNearPlaneDistance(Math.max(28.0f, far * 0.35f));
                event.setFarPlaneDistance(Math.max(75.0f, far * 0.85f));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null || mc.player == null) {
            return;
        }

        // Screen vignette overlay: smooth circular cinematic vignette with breathing heartbeat pulse
        if (EnjoyNightsConfig.CLIENT.bloodMoonScreenVignette.get() && NightAndMoonHelper.isBloodMoon(level)) {
            GuiGraphics graphics = event.getGuiGraphics();
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();

            // Smooth breathing / heartbeat pulse (period ~ 3.5 seconds)
            long gameTime = level.getGameTime();
            float pulse = (float) (Math.sin(gameTime * 0.08) * 0.5 + 0.5);

            float minIntensity;
            float maxIntensity;

            if (EnjoyNightsConfig.CLIENT.bloodMoonComplementarySpecific.get() && ShaderDetectionHelper.isComplementaryActive()) {
                // Complementary Unbound calibration: subtle corner framing, clean view in center
                minIntensity = 0.50F;
                maxIntensity = 0.75F;
            } else {
                // Vanilla / standard shaders:
                minIntensity = 0.45F;
                maxIntensity = 0.70F;
            }

            // User config modifier allows scaling the overall intensity
            float configMultiplier = EnjoyNightsConfig.CLIENT.bloodMoonVignetteIntensity.get().floatValue() / 0.70F;
            float intensity = Mth.clamp((minIntensity + (maxIntensity - minIntensity) * pulse) * configMultiplier, 0.20F, 1.0F);

            // Modulate texture alpha with breathing pulse; RGB = 1.0 preserves texture's rich crimson
            int color = net.minecraft.util.ARGB.colorFromFloat(intensity, 1.0F, 1.0F, 1.0F);

            graphics.blit(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    BLOOD_VIGNETTE_LOCATION,
                    0, 0,
                    0.0F, 0.0F,
                    width, height,
                    width, height,
                    color
            );
        }
    }

    private static BloodMoonAmbientSoundInstance bloodMoonAmbientSound = null;
    private static ClientLevel lastInjectedLevel = null;

    private static void ensureSkyColorLayer(ClientLevel level) {
        if (level == lastInjectedLevel) {
            return;
        }
        lastInjectedLevel = level;

        try {
            net.minecraft.world.attribute.EnvironmentAttributeSystem system = level.environmentAttributes();
            for (java.lang.reflect.Field f : net.minecraft.world.attribute.EnvironmentAttributeSystem.class.getDeclaredFields()) {
                if (java.util.Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    java.util.Map<?, ?> samplers = (java.util.Map<?, ?>) f.get(system);
                    if (samplers != null) {
                        Object skySampler = samplers.get(net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR);
                        if (skySampler != null) {
                            for (java.lang.reflect.Field sf : skySampler.getClass().getDeclaredFields()) {
                                if (java.util.List.class.isAssignableFrom(sf.getType())) {
                                    sf.setAccessible(true);
                                    java.util.List<net.minecraft.world.attribute.EnvironmentAttributeLayer<Integer>> layers =
                                            (java.util.List<net.minecraft.world.attribute.EnvironmentAttributeLayer<Integer>>) sf.get(skySampler);
                                    layers.add((net.minecraft.world.attribute.EnvironmentAttributeLayer.Constant<Integer>) baseColor -> {
                                        if (NightAndMoonHelper.isBloodMoon(Minecraft.getInstance().level)) {
                                            // Red sky dome is ONLY active when shader packs are in use.
                                            // In vanilla, baseColor is preserved so vanilla sky is natural and not solid red.
                                            if (ShaderDetectionHelper.isShaderPackInUse()) {
                                                return net.minecraft.util.ARGB.color(255, 205, 22, 22);
                                            }
                                        }
                                        return baseColor;
                                    });
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.level == null || mc.player == null) {
            return;
        }

        ClientLevel level = mc.level;
        ensureSkyColorLayer(level);
        LocalPlayer player = mc.player;
        boolean isBloodMoon = NightAndMoonHelper.isBloodMoon(level);

        // Manage continuous Basalt Deltas ambient loop
        if (isBloodMoon && EnjoyNightsConfig.CLIENT.paranoiaSoundsEnabled.get()) {
            if (bloodMoonAmbientSound == null || bloodMoonAmbientSound.isStopped()) {
                bloodMoonAmbientSound = new BloodMoonAmbientSoundInstance(
                        SoundEvents.AMBIENT_BASALT_DELTAS_LOOP.value(),
                        SoundSource.AMBIENT,
                        level.getRandom()
                );
                mc.getSoundManager().play(bloodMoonAmbientSound);
            }
        } else {
            if (bloodMoonAmbientSound != null) {
                if (!bloodMoonAmbientSound.isStopping()) {
                    bloodMoonAmbientSound.startFadeOut();
                }
                if (bloodMoonAmbientSound.isStopped()) {
                    bloodMoonAmbientSound = null;
                }
            }
        }

        if (!isBloodMoon) {
            ambientSoundCooldown = 900;
            phantomFootstepsCooldown = 60;
            phantomStepsRemaining = 0;
            phantomStepPos = null;
            phantomStepSound = null;
            return;
        }

        RandomSource random = level.getRandom();

        // 1. Ambient Nether Spore Atmosphere (Cobalt Warped Spores & Crimson Spores)
        if (EnjoyNightsConfig.CLIENT.bloodMoonParticles.get()) {
            // 1.1 Ambient cobalt spores (Warped Forest effect - ethereal floating cobalt motes)
            for (int i = 0; i < 4; i++) {
                double px = player.getX() + (random.nextDouble() - 0.5) * 16.0;
                double py = player.getY() + (random.nextDouble() - 0.1) * 7.0;
                double pz = player.getZ() + (random.nextDouble() - 0.5) * 16.0;
                level.addParticle(ParticleTypes.WARPED_SPORE, px, py, pz, 0.0, 0.0, 0.0);
            }

            // 1.2 Crimson spores (blood-red floating motes)
            for (int i = 0; i < 2; i++) {
                double px = player.getX() + (random.nextDouble() - 0.5) * 16.0;
                double py = player.getY() + (random.nextDouble() - 0.1) * 7.0;
                double pz = player.getZ() + (random.nextDouble() - 0.5) * 16.0;
                level.addParticle(ParticleTypes.CRIMSON_SPORE, px, py, pz, 0.0, 0.0, 0.0);
            }

            // 1.3 Subtle falling ash fallout from the dark sky
            if (random.nextBoolean()) {
                double px = player.getX() + (random.nextDouble() - 0.5) * 14.0;
                double py = player.getY() + 2.0 + random.nextDouble() * 2.5;
                double pz = player.getZ() + (random.nextDouble() - 0.5) * 14.0;
                level.addParticle(
                        ParticleTypes.ASH,
                        px, py, pz,
                        (random.nextDouble() - 0.5) * 0.03,
                        -0.03,
                        (random.nextDouble() - 0.5) * 0.03
                );
            }

            // 1.4 Occasional dark ground mist wisps
            if (random.nextInt(4) == 0) {
                double px = player.getX() + (random.nextDouble() - 0.5) * 8.0;
                double py = player.getY() + 0.1;
                double pz = player.getZ() + (random.nextDouble() - 0.5) * 8.0;
                level.addParticle(
                        ParticleTypes.SMOKE,
                        px, py, pz,
                        (random.nextDouble() - 0.5) * 0.01,
                        0.01 + random.nextDouble() * 0.02,
                        (random.nextDouble() - 0.5) * 0.01
                );
            }
        }

        // 2. Psychological Horror Ambient Soundscape
        // Spaced out to 45 to 80 seconds (900 to 1600 ticks) so it startles without annoying
        if (EnjoyNightsConfig.CLIENT.paranoiaSoundsEnabled.get()) {
            ambientSoundCooldown--;
            if (ambientSoundCooldown <= 0) {
                ambientSoundCooldown = 900 + random.nextInt(700);

                int choice = random.nextInt(4);
                switch (choice) {
                    case 0 -> {
                        // Ominous Warden heartbeat thump
                        level.playLocalSound(player, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 0.9F, 0.65F);
                    }
                    case 1 -> {
                        // Deep, heavy funeral bell echoing in the distance
                        level.playLocalSound(player, SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 0.8F, 0.45F);
                    }
                    case 2 -> {
                        // Haunting cave ambience / cosmic drone
                        level.playLocalSound(player, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.85F, 0.55F);
                    }
                    case 3 -> {
                        // Phantom creeper primed fuse hiss right behind or near the player
                        float angle = player.getYRot() + 180.0F + (random.nextFloat() - 0.5F) * 60.0F;
                        double rad = Math.toRadians(angle);
                        double dist = 2.5 + random.nextDouble() * 2.0;
                        double sx = player.getX() - Math.sin(rad) * dist;
                        double sz = player.getZ() + Math.cos(rad) * dist;
                        double sy = player.getY();
                        level.playLocalSound(sx, sy, sz, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 0.80F, 1.0F, false);
                    }
                }
            }
        }

        // 3. Phantom Footsteps Rushing Behind Player
        if (EnjoyNightsConfig.CLIENT.bloodMoonPhantomFootstepsEnabled.get()) {
            // 3.1 Progress any active multi-step sequence
            if (phantomStepsRemaining > 0) {
                phantomStepDelay--;
                if (phantomStepDelay <= 0) {
                    phantomStepsRemaining--;
                    phantomStepDelay = 5 + random.nextInt(3); // 5 to 7 ticks between steps (rapid pacing)

                    if (phantomStepPos != null && phantomStepSound != null) {
                        level.playLocalSound(
                                phantomStepPos.x, phantomStepPos.y, phantomStepPos.z,
                                phantomStepSound,
                                SoundSource.PLAYERS,
                                1.35F,
                                0.95F + (random.nextFloat() - 0.5F) * 0.15F,
                                false
                        );

                        // Close distance towards player by ~0.6 blocks per step
                        Vec3 dirToPlayer = player.position().subtract(phantomStepPos);
                        if (dirToPlayer.lengthSqr() > 0.01) {
                            phantomStepPos = phantomStepPos.add(dirToPlayer.normalize().scale(0.6));
                        }
                    }
                }
            }

            // 3.2 Movement tracking & trigger detection
            Vec3 movement = player.getDeltaMovement();
            double currentSpeedSqr = movement.x * movement.x + movement.z * movement.z;
            boolean justStopped = (lastPlayerSpeedSqr > 0.005 && currentSpeedSqr < 0.001);
            boolean isMining = (mc.gameMode != null && mc.gameMode.isDestroying())
                    || (mc.options.keyAttack.isDown() && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK);
            boolean nearItem = currentSpeedSqr < 0.002
                    && !level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2.5)).isEmpty();
            boolean standingStill = currentSpeedSqr < 0.001 && random.nextInt(35) == 0;
            lastPlayerSpeedSqr = currentSpeedSqr;

            if (phantomStepsRemaining == 0) {
                if (phantomFootstepsCooldown > 0) {
                    phantomFootstepsCooldown--;
                } else {
                    // Trigger when player stops walking, stops to pick up an item, is actively mining, or is paused still
                    if (justStopped || nearItem || isMining || standingStill) {
                        // Reset cooldown: 50 to 90 seconds (1000 to 1800 ticks)
                        phantomFootstepsCooldown = 1000 + random.nextInt(800);

                        // Calculate position 1.8 to 2.4 blocks directly behind player's back
                        float angle = player.getYRot() + 180.0F + (random.nextFloat() - 0.5F) * 30.0F;
                        double rad = Math.toRadians(angle);
                        double dist = 1.8 + random.nextDouble() * 0.6;
                        double sx = player.getX() - Math.sin(rad) * dist;
                        double sz = player.getZ() + Math.cos(rad) * dist;
                        double sy = player.getY();

                        // Match step sound to the floor material (wood on wood, grass on grass, etc.)
                        BlockPos groundPos = BlockPos.containing(sx, sy - 0.2, sz);
                        BlockState state = level.getBlockState(groundPos);
                        if (state.isAir() || !state.blocksMotion()) {
                            groundPos = player.blockPosition().below();
                            state = level.getBlockState(groundPos);
                        }

                        SoundEvent stepSound = state.getSoundType().getStepSound();
                        if (stepSound == null) {
                            stepSound = SoundEvents.GRASS_STEP;
                        }

                        phantomStepSound = stepSound;
                        phantomStepPos = new Vec3(sx, sy, sz);
                        phantomStepsRemaining = 2 + random.nextInt(2); // 2 or 3 rapid footsteps
                        phantomStepDelay = 0; // First step triggers immediately
                    }
                }
            }
        }
    }
}
