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

        // Register Deferred Registers
        com.coderdiaz.enjoynights.init.ModBlocks.BLOCKS.register(modEventBus);
        com.coderdiaz.enjoynights.init.ModItems.ITEMS.register(modEventBus);
        com.coderdiaz.enjoynights.init.ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        com.coderdiaz.enjoynights.init.ModMenuTypes.MENUS.register(modEventBus);

        // Register Creative Tab Contents
        modEventBus.addListener(this::addCreative);

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
            modEventBus.addListener(this::registerScreens);
        }

        LOGGER.info("Enjoy nights! initialized successfully.");
    }

    private void addCreative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(com.coderdiaz.enjoynights.init.ModItems.WARDING_TOTEM.get());
            event.accept(com.coderdiaz.enjoynights.init.ModItems.CRIMSON_ALTAR.get());
        }
    }

    private void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(
                com.coderdiaz.enjoynights.init.ModMenuTypes.WARDING_TOTEM.get(),
                com.coderdiaz.enjoynights.client.gui.WardingTotemScreen::new
        );
    }

    private void registerCommands(RegisterCommandsEvent event) {
        EnjoyNightsCommands.register(event.getDispatcher());
    }
}
