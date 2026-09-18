package com.coderdiaz.enjoynights.client;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public class ClientEvents {

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null) {
            return;
        }

        // Blood Moon blood-red fog effect (vanilla)
        if (EnjoyNightsConfig.CLIENT.bloodMoonFogTint.get() && NightAndMoonHelper.isBloodMoon(level)) {
            float red = Math.min(1.0f, event.getRed() * 1.8f + 0.45f);
            float green = event.getGreen() * 0.20f;
            float blue = event.getBlue() * 0.20f;

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

        // Brings fog closer in vanilla rendering so the red mist is thick and visible
        if (EnjoyNightsConfig.CLIENT.bloodMoonFogTint.get() && NightAndMoonHelper.isBloodMoon(level)) {
            event.setNearPlaneDistance(14.0f);
            event.setFarPlaneDistance(52.0f);
        }
    }

    private static final net.minecraft.resources.Identifier VIGNETTE_LOCATION =
            net.minecraft.resources.Identifier.withDefaultNamespace("textures/misc/vignette.png");

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null || mc.player == null) {
            return;
        }

        // Screen vignette overlay: smooth circular cinematic vignette without seams or full-screen wash
        if (EnjoyNightsConfig.CLIENT.bloodMoonScreenVignette.get() && NightAndMoonHelper.isBloodMoon(level)) {
            GuiGraphics graphics = event.getGuiGraphics();
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();

            float intensity = EnjoyNightsConfig.CLIENT.bloodMoonVignetteIntensity.get().floatValue();

            // Specific calibration for Complementary Unbound:
            // Complementary has aggressive high-contrast tonemapping, so a softer intensity creates an elegant
            // dark crimson horizon fade without darkening or washing out the screen center.
            if (EnjoyNightsConfig.CLIENT.bloodMoonComplementarySpecific.get() && ShaderDetectionHelper.isComplementaryActive()) {
                intensity = Math.min(intensity, 0.45F);
            }

            // Using Mojang's native RenderPipelines.VIGNETTE with blend Dst * (1 - Src):
            // Green and blue channels are multiplied by (1 - intensity) on the screen borders,
            // producing an authentic dark-crimson cinematic vignette with zero vertical lines,
            // no washed out veil, and leaving the center of the screen crystal clear.
            int color = net.minecraft.util.ARGB.colorFromFloat(1.0F, 0.0F, intensity, intensity);

            graphics.blit(
                    net.minecraft.client.renderer.RenderPipelines.VIGNETTE,
                    VIGNETTE_LOCATION,
                    0, 0,
                    0.0F, 0.0F,
                    width, height,
                    width, height,
                    color
            );
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.level == null || mc.player == null) {
            return;
        }

        Level level = mc.level;
        if (EnjoyNightsConfig.CLIENT.bloodMoonParticles.get() && NightAndMoonHelper.isBloodMoon(level)) {
            // Spawn subtle floating crimson ember particles around the player
            RandomSource random = level.getRandom();
            for (int i = 0; i < 2; i++) {
                double px = mc.player.getX() + (random.nextDouble() - 0.5) * 16.0;
                double py = mc.player.getY() + random.nextDouble() * 6.0;
                double pz = mc.player.getZ() + (random.nextDouble() - 0.5) * 16.0;

                level.addParticle(
                        ParticleTypes.CRIMSON_SPORE,
                        px, py, pz,
                        (random.nextDouble() - 0.5) * 0.04,
                        -0.015,
                        (random.nextDouble() - 0.5) * 0.04
                );
            }
        }
    }
}
