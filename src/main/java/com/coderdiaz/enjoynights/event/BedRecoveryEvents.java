package com.coderdiaz.enjoynights.event;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.BedSleepHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BedRecoveryEvents {
    private static final Map<UUID, Integer> PLAYER_SLEEP_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PLAYER_LAST_REST_TICK = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        long currentTick = player.level().getGameTime();
        long lastRest = PLAYER_LAST_REST_TICK.getOrDefault(player.getUUID(), 0L);
        long cooldownTicks = EnjoyNightsConfig.SERVER.bedRestCooldownSeconds.get() * 20L;

        if (currentTick - lastRest < cooldownTicks) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            long remainingSeconds = (cooldownTicks - (currentTick - lastRest) + 19) / 20;

            player.displayClientMessage(
                    Component.translatable("enjoy_nights.bed.cooldown", remainingSeconds)
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onSleepFinishedTime(SleepFinishedTimeEvent event) {
        if (EnjoyNightsConfig.SERVER.preventNightSkip.get()) {
            // Prevent daylight advancement by setting wake-up time to current dayTime
            if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
                event.setTimeAddition(level.getDayTime());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();

        if (player.isSleeping()) {
            if (EnjoyNightsConfig.SERVER.preventNightSkip.get()) {
                BedSleepHelper.clampVanillaSleepCounter(player);
            }

            int currentTicks = PLAYER_SLEEP_TICKS.compute(uuid, (id, ticks) -> ticks == null ? 1 : ticks + 1);
            int maxTicks = EnjoyNightsConfig.SERVER.bedSleepDurationSeconds.get() * 20;

            if (currentTicks >= maxTicks) {
                // Auto-wake up after resting duration
                player.stopSleeping();
                PLAYER_SLEEP_TICKS.remove(uuid);

                // Record cooldown timestamp
                PLAYER_LAST_REST_TICK.put(uuid, player.level().getGameTime());

                // Apply the recovery buffs (Regeneration II, Resistance I, Saturation)
                BedSleepHelper.applyRecoveryBuffs(player);
            }
        } else {
            PLAYER_SLEEP_TICKS.remove(uuid);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYER_SLEEP_TICKS.remove(event.getEntity().getUUID());
    }
}
