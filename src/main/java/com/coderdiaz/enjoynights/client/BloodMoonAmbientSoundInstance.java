package com.coderdiaz.enjoynights.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Continuous looping ambient sound instance for the Blood Moon event.
 * Plays basalt deltas ambient rumbling with smooth fade-in and fade-out.
 */
public class BloodMoonAmbientSoundInstance extends AbstractTickableSoundInstance {

    private static final float TARGET_VOLUME = 0.85F;
    private static final int FADE_TICKS = 40; // 2 seconds fade

    private boolean stopping = false;
    private int fadeTimer = 0;

    public BloodMoonAmbientSoundInstance(SoundEvent soundEvent, SoundSource soundSource, RandomSource random) {
        super(soundEvent, soundSource, random);
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 0.95F;
        this.relative = true;
    }

    public void startFadeOut() {
        this.stopping = true;
    }

    public boolean isStopping() {
        return this.stopping;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            this.stop();
            return;
        }

        if (this.stopping) {
            this.fadeTimer--;
            if (this.fadeTimer <= 0) {
                this.stop();
                return;
            }
        } else {
            if (this.fadeTimer < FADE_TICKS) {
                this.fadeTimer++;
            }
        }

        float progress = (float) this.fadeTimer / (float) FADE_TICKS;
        this.volume = Mth.clamp(progress * TARGET_VOLUME, 0.0F, TARGET_VOLUME);
    }
}
