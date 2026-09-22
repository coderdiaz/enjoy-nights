package com.coderdiaz.enjoynights.init;

import com.coderdiaz.enjoynights.EnjoyNights;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnjoyNights.MOD_ID);

    public static final DeferredItem<BlockItem> WARDING_TOTEM = ITEMS.registerSimpleBlockItem(
            ModBlocks.WARDING_TOTEM,
            props -> props.rarity(Rarity.RARE).stacksTo(16)
    );

    public static final DeferredItem<BlockItem> CRIMSON_ALTAR = ITEMS.registerSimpleBlockItem(
            ModBlocks.CRIMSON_ALTAR,
            props -> props.rarity(Rarity.EPIC).stacksTo(16)
    );
}
