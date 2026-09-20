package com.coderdiaz.enjoynights.init;

import com.coderdiaz.enjoynights.EnjoyNights;
import com.coderdiaz.enjoynights.menu.WardingTotemMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
            Registries.MENU,
            EnjoyNights.MOD_ID
    );

    public static final Supplier<MenuType<WardingTotemMenu>> WARDING_TOTEM = MENUS.register(
            "warding_totem",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new WardingTotemMenu(windowId, inv, data.readBlockPos()))
    );
}
