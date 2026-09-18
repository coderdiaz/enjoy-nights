package com.coderdiaz.enjoynights.util;

import com.coderdiaz.enjoynights.EnjoyNights;
import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

public class BedSleepHelper {
    private static Field sleepCounterField = null;
    private static boolean reflectionAttempted = false;

    private static void initReflection() {
        if (reflectionAttempted) return;
        reflectionAttempted = true;
        try {
            sleepCounterField = Player.class.getDeclaredField("sleepCounter");
            sleepCounterField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            EnjoyNights.LOGGER.warn("Could not access sleepCounter field directly, trying alternative lookup: {}", e.getMessage());
            for (Field field : Player.class.getDeclaredFields()) {
                if (field.getType() == int.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    if (field.getName().equalsIgnoreCase("sleepCounter") || field.getName().equals("f_36081_")) {
                        sleepCounterField = field;
                        sleepCounterField.setAccessible(true);
                        break;
                    }
                }
            }
        }
    }

    public static void clampVanillaSleepCounter(Player player) {
        initReflection();
        if (sleepCounterField != null) {
            try {
                int current = sleepCounterField.getInt(player);
                if (current >= 80) {
                    sleepCounterField.setInt(player, 40);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void applyRecoveryBuffs(ServerPlayer player) {
        int durationSeconds = EnjoyNightsConfig.SERVER.bedBuffDurationSeconds.get();
        int durationTicks = durationSeconds * 20;

        // Stamina & Recovery buffs
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, durationTicks, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durationTicks, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false, false));

        // Play pleasant rest sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);

        // Notify player
        player.displayClientMessage(
                Component.translatable("enjoy_nights.bed.rest_message")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                true
        );
    }
}
