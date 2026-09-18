package com.coderdiaz.enjoynights.event;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class PhantomSuppressionEvents {

    @SubscribeEvent
    public static void onPhantomSpawn(FinalizeSpawnEvent event) {
        if (!EnjoyNightsConfig.SERVER.preventPhantoms.get()) {
            return;
        }

        if (event.getEntity() instanceof Phantom) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!EnjoyNightsConfig.SERVER.preventPhantoms.get()) {
            return;
        }

        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Phantom) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!EnjoyNightsConfig.SERVER.preventPhantoms.get()) {
            return;
        }

        Player entity = event.getEntity();
        if (entity instanceof ServerPlayer player && player.tickCount % 100 == 0) {
            // Keep insomnia timer at 0 so vanilla phantom spawner never queues phantoms
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        }
    }
}
