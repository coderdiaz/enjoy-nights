package com.coderdiaz.enjoynights.util;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import net.minecraft.world.level.Level;

public class NightAndMoonHelper {
    public static final int MOON_PHASE_FULL = 0;
    public static final int MOON_PHASE_NEW = 4;

    private static boolean forceBloodMoon = false;

    public static boolean isNight(Level level) {
        long timeOfDay = level.getDayTime() % 24000L;
        return timeOfDay >= 12500L && timeOfDay <= 23500L;
    }

    public static long getDay(Level level) {
        return level.getDayTime() / 24000L;
    }

    public static int getMoonPhase(Level level) {
        return (int) (level.getDayTime() / 24000L % 8L + 8L) % 8;
    }

    public static boolean isFullMoon(Level level) {
        return isNight(level) && getMoonPhase(level) == MOON_PHASE_FULL;
    }

    public static boolean isNewMoon(Level level) {
        return isNight(level) && getMoonPhase(level) == MOON_PHASE_NEW;
    }

    public static boolean isBloodMoon(Level level) {
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
        long currentDay = getDay(level);
        int interval = EnjoyNightsConfig.SERVER.bloodMoonIntervalDays.get();
        if (interval <= 0) return 0;
        long next = ((currentDay / interval) + 1) * interval;
        return next - currentDay;
    }
}
