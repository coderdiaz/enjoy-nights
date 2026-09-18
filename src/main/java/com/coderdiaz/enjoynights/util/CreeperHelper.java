package com.coderdiaz.enjoynights.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;

import java.lang.reflect.Field;

public class CreeperHelper {
    private static EntityDataAccessor<Boolean> DATA_IS_POWERED = null;
    private static boolean initialized = false;

    @SuppressWarnings("unchecked")
    private static void init() {
        if (initialized) return;
        initialized = true;

        try {
            Field field = Creeper.class.getDeclaredField("DATA_IS_POWERED");
            field.setAccessible(true);
            DATA_IS_POWERED = (EntityDataAccessor<Boolean>) field.get(null);
        } catch (Exception e) {
            for (Field f : Creeper.class.getDeclaredFields()) {
                if (EntityDataAccessor.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(null);
                        if (val instanceof EntityDataAccessor<?> accessor) {
                            // Check if accessor holds Boolean
                            DATA_IS_POWERED = (EntityDataAccessor<Boolean>) accessor;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public static void setPowered(Creeper creeper, boolean powered) {
        init();
        if (DATA_IS_POWERED != null) {
            creeper.getEntityData().set(DATA_IS_POWERED, powered);
        }
    }
}
