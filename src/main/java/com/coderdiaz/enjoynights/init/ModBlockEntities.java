package com.coderdiaz.enjoynights.init;

import com.coderdiaz.enjoynights.EnjoyNights;
import com.coderdiaz.enjoynights.block.entity.WardingTotemBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE,
            EnjoyNights.MOD_ID
    );

    public static final Supplier<BlockEntityType<WardingTotemBlockEntity>> WARDING_TOTEM = BLOCK_ENTITIES.register(
            "warding_totem",
            () -> new BlockEntityType<>(
                    WardingTotemBlockEntity::new,
                    ModBlocks.WARDING_TOTEM.get()
            )
    );
}
