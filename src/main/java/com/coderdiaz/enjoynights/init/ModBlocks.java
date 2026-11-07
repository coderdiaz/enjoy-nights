package com.coderdiaz.enjoynights.init;

import com.coderdiaz.enjoynights.EnjoyNights;
import com.coderdiaz.enjoynights.block.WardingTotemBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnjoyNights.MOD_ID);

    public static final DeferredBlock<WardingTotemBlock> WARDING_TOTEM = BLOCKS.registerBlock(
            "warding_totem",
            WardingTotemBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(WardingTotemBlock.ACTIVE) ? 10 : 2)
                    .noLootTable()
                    .requiresCorrectToolForDrops()
    );
}
