package com.coderdiaz.enjoynights.event;

import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public class EnhancedDropsEvents {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!EnjoyNightsConfig.SERVER.fullMoonExtraDropsEnabled.get()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (level.isClientSide()) {
            return;
        }

        // Only for standard monsters during Full Moon or Blood Moon nights
        if (!(entity instanceof Monster)) {
            return;
        }

        boolean isFullMoon = NightAndMoonHelper.isFullMoon(level);
        boolean isBloodMoon = NightAndMoonHelper.isBloodMoon(level);

        if (!isFullMoon && !isBloodMoon) {
            return;
        }

        RandomSource random = entity.getRandom();

        // 1% chance for Diamond (configurable)
        double diamondChance = EnjoyNightsConfig.SERVER.fullMoonDiamondDropChance.get();
        if (random.nextFloat() < diamondChance) {
            addDrop(event, entity, new ItemStack(Items.DIAMOND, 1));
        }

        // Mineral fragments: Iron
        float ironRoll = random.nextFloat();
        if (ironRoll < 0.35f) {
            addDrop(event, entity, new ItemStack(Items.RAW_IRON, 1 + random.nextInt(2)));
        } else if (ironRoll < 0.60f) {
            addDrop(event, entity, new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(4)));
        }

        // Mineral fragments: Gems (Lapis, Amethyst, Emerald)
        float gemRoll = random.nextFloat();
        if (gemRoll < 0.20f) {
            addDrop(event, entity, new ItemStack(Items.LAPIS_LAZULI, 1 + random.nextInt(4)));
        } else if (gemRoll < 0.35f) {
            addDrop(event, entity, new ItemStack(Items.AMETHYST_SHARD, 1 + random.nextInt(2)));
        } else if (gemRoll < 0.43f) {
            addDrop(event, entity, new ItemStack(Items.EMERALD, 1));
        }
    }

    private static void addDrop(LivingDropsEvent event, LivingEntity entity, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(
                entity.level(),
                entity.getX(),
                entity.getY() + 0.5,
                entity.getZ(),
                stack
        );
        itemEntity.setDefaultPickUpDelay();
        event.getDrops().add(itemEntity);
    }
}
