package com.coderdiaz.enjoynights;

import com.coderdiaz.enjoynights.client.ClientEvents;
import com.coderdiaz.enjoynights.command.EnjoyNightsCommands;
import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.event.BedRecoveryEvents;
import com.coderdiaz.enjoynights.event.BloodMoonEvents;
import com.coderdiaz.enjoynights.event.EnhancedDropsEvents;
import com.coderdiaz.enjoynights.event.LunarPhaseEvents;
import com.coderdiaz.enjoynights.event.ParanoiaEvents;
import com.coderdiaz.enjoynights.event.PhantomSuppressionEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EnjoyNights.MOD_ID)
public class EnjoyNights {
    public static final String MOD_ID = "enjoy_nights";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public EnjoyNights(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Enjoy nights! for Minecraft 1.21.11 / NeoForge 21.11.45...");

        // Register Configs
        modContainer.registerConfig(ModConfig.Type.SERVER, EnjoyNightsConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, EnjoyNightsConfig.CLIENT_SPEC);

        // Register Common Gameplay Events to NeoForge Event Bus
        NeoForge.EVENT_BUS.register(BedRecoveryEvents.class);
        NeoForge.EVENT_BUS.register(PhantomSuppressionEvents.class);
        NeoForge.EVENT_BUS.register(EnhancedDropsEvents.class);
        NeoForge.EVENT_BUS.register(LunarPhaseEvents.class);
        NeoForge.EVENT_BUS.register(ParanoiaEvents.class);
        NeoForge.EVENT_BUS.register(BloodMoonEvents.class);

        // Register Commands
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Register Client Events
        if (FMLEnvironment.getDist().isClient()) {
            NeoForge.EVENT_BUS.register(ClientEvents.class);
        }

        LOGGER.info("Enjoy nights! initialized successfully.");
    }

    private void registerCommands(RegisterCommandsEvent event) {
        EnjoyNightsCommands.register(event.getDispatcher());
    }
}
