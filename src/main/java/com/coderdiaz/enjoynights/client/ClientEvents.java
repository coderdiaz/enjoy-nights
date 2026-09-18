package com.coderdiaz.enjoynights.client;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public class ClientEvents {

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null) {
            return;
        }

        // Blood Moon blood-red fog effect
        if (EnjoyNightsConfig.CLIENT.bloodMoonFogTint.get() && NightAndMoonHelper.isBloodMoon(level)) {
            float red = Math.min(1.0f, event.getRed() * 1.6f + 0.35f);
            float green = event.getGreen() * 0.30f;
            float blue = event.getBlue() * 0.30f;

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
}
