package com.starforge.registry;

import com.starforge.Starforge;
import com.starforge.uncrafting.UncraftingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * 容器菜单注册。
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Starforge.MOD_ID);

    public static final Supplier<MenuType<UncraftingMenu>> UNCRAFTING_TABLE = MENUS.register(
            "uncrafting_table",
            () -> new MenuType<>(UncraftingMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }
}
