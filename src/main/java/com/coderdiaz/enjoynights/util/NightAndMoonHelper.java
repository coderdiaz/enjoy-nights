package com.coderdiaz.enjoynights.util;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import net.minecraft.world.level.Level;

public class NightAndMoonHelper {
    public static final int MOON_PHASE_FULL = 0;
    public static final int MOON_PHASE_NEW = 4;

    /** Time-of-day tick at which dusk begins and night mechanics activate. */
    public static final long DUSK_TIME = 12500L;
    /** Number of ticks in a full Minecraft day/night cycle. */
    public static final long TICKS_PER_DAY = 24000L;

    private static boolean forceBloodMoon = false;

    public static boolean isNight(Level level) {
        if (level == null) return false;
        long timeOfDay = level.getDayTime() % TICKS_PER_DAY;
        return timeOfDay >= DUSK_TIME && timeOfDay <= 23500L;
    }

    /**
     * Returns the time of day in ticks (0 to 23999), where 0 is sunrise.
     */
    public static long getTimeOfDay(Level level) {
        if (level == null) return 0;
        return level.getDayTime() % TICKS_PER_DAY;
    }

    /**
     * Computes the moon phase (0 to 7) for an arbitrary absolute day number.
     * Phase 0 is Full Moon, phase 4 is New Moon. This mirrors the vanilla
     * calculation but allows predicting the phase of a future/past day without
     * requiring it to currently be night.
     */
    public static int getMoonPhaseForDay(long day) {
        return (int) ((day % 8L + 8L) % 8L);
    }

    /**
     * Whether the given absolute day number will be a Blood Moon night: either the
     * configured interval schedule lands on this day, or a Blood Moon is currently
     * being forced. Independent of the current time of day, so it can be used to
     * predict whether "tonight" will be a Blood Moon while it is still daytime.
     */
    public static boolean isBloodMoonDayNumber(Level level, long day) {
        if (level == null) return false;
        if (!EnjoyNightsConfig.SERVER.bloodMoonSiegeEnabled.get()) {
            return false;
        }
        // A forced Blood Moon turns tonight into a Blood Moon regardless of the schedule.
        if (forceBloodMoon) {
            return true;
        }
        int interval = EnjoyNightsConfig.SERVER.bloodMoonIntervalDays.get();
        if (interval <= 0) return false;
        return day > 0 && (day % interval == 0);
    }

    /**
     * Whether the current time sits within the pre-dusk window: the final
     * {@code leadTicks} ticks of daytime right before night falls at {@link #DUSK_TIME}.
     */
    public static boolean isInPreDuskWindow(Level level, int leadTicks) {
        if (level == null || leadTicks <= 0) return false;
        long timeOfDay = getTimeOfDay(level);
        return timeOfDay >= DUSK_TIME - leadTicks && timeOfDay < DUSK_TIME;
    }

    public static long getDay(Level level) {
        if (level == null) return 0;
        return level.getDayTime() / TICKS_PER_DAY;
    }

    public static int getMoonPhase(Level level) {
        if (level == null) return 0;
        return (int) (level.getDayTime() / TICKS_PER_DAY % 8L + 8L) % 8;
    }

    public static boolean isFullMoon(Level level) {
        return isNight(level) && getMoonPhase(level) == MOON_PHASE_FULL;
    }

    public static boolean isNewMoon(Level level) {
        return isNight(level) && getMoonPhase(level) == MOON_PHASE_NEW;
    }

    public static boolean isBloodMoon(Level level) {
        if (level == null) {
            return false;
        }

        if (!EnjoyNightsConfig.SERVER.bloodMoonSiegeEnabled.get()) {
            return false;
        }

        if (forceBloodMoon && isNight(level)) {
            return true;
        }

        if (!isNight(level)) {
            return false;
        }

        long day = getDay(level);
        int interval = EnjoyNightsConfig.SERVER.bloodMoonIntervalDays.get();
        return day > 0 && (day % interval == 0);
    }

    public static void setForceBloodMoon(boolean force) {
        forceBloodMoon = force;
    }

    public static boolean isForceBloodMoon() {
        return forceBloodMoon;
    }

    public static long getDaysUntilNextBloodMoon(Level level) {
        if (level == null) return 0;
        long currentDay = getDay(level);
        int interval = EnjoyNightsConfig.SERVER.bloodMoonIntervalDays.get();
        if (interval <= 0) return 0;
        long next = ((currentDay / interval) + 1) * interval;
        return next - currentDay;
    }
}
