package com.coderdiaz.enjoynights.event;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParanoiaEvents {
    private static final Map<UUID, ParanoiaTracker> TRACKERS = new ConcurrentHashMap<>();

    private static class ParanoiaTracker {
        Vec3 lastPos;
        int stillTicks = 0;
        int soundCooldown = 0;

        ParanoiaTracker(Vec3 pos) {
            this.lastPos = pos;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!EnjoyNightsConfig.SERVER.paranoiaWeaknessEnabled.get()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        UUID uuid = player.getUUID();
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        ParanoiaTracker tracker = TRACKERS.computeIfAbsent(uuid, k -> new ParanoiaTracker(player.position()));

        boolean isNight = NightAndMoonHelper.isNight(level);
        boolean noBlockLight = level.getBrightness(LightLayer.BLOCK, pos) == 0;
        boolean isClosedArea = !level.canSeeSky(pos) || level.getBrightness(LightLayer.SKY, pos) < 4;
        boolean isStill = player.position().distanceToSqr(tracker.lastPos) < 0.09;

        tracker.lastPos = player.position();

        if (isNight && noBlockLight && isClosedArea && isStill) {
            tracker.stillTicks += 20;

            int thresholdTicks = EnjoyNightsConfig.SERVER.paranoiaDurationSeconds.get() * 20;

            if (tracker.stillTicks >= thresholdTicks) {
                // Apply weakness
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, true, true));

                // Hallucination darkness/nausea
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, true, false, false));

                // Play disturbing sounds every ~5-10 seconds
                if (tracker.soundCooldown <= 0) {
                    playDisturbingSound(player);
                    tracker.soundCooldown = 100 + player.getRandom().nextInt(100);

                    // Whisper in actionbar
                    player.displayClientMessage(
                            Component.translatable("enjoy_nights.paranoia.whisper")
                                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                            true
                    );
                } else {
                    tracker.soundCooldown -= 20;
                }
            }
        } else {
            // Player moved or introduced light: paranoia decreases
            tracker.stillTicks = Math.max(0, tracker.stillTicks - 40);
            if (tracker.soundCooldown > 0) {
                tracker.soundCooldown -= 20;
            }
        }
    }

    private static void playDisturbingSound(ServerPlayer player) {
        SoundEvent[] creepySounds = new SoundEvent[]{
                SoundEvents.AMBIENT_CAVE.value(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundEvents.PHANTOM_SWOOP,
                SoundEvents.ZOMBIE_AMBIENT,
                SoundEvents.CREEPER_PRIMED
        };

        SoundEvent chosen = creepySounds[player.getRandom().nextInt(creepySounds.length)];
        float pitch = 0.7f + player.getRandom().nextFloat() * 0.5f;

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                chosen,
                SoundSource.AMBIENT,
                0.8f,
                pitch
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKERS.remove(event.getEntity().getUUID());
    }
}
