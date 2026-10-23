package com.starforge.registry;

import com.starforge.Starforge;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A dedicated creative tab holding every item of the mod.
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Starforge.MOD_ID);

    public static final Supplier<CreativeModeTab> STARFORGE = TABS.register("starforge",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.starforge"))
                    .icon(() -> new ItemStack(ModItems.STARFORGE_INGOT.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.WHISPER.get());
                        output.accept(ModItems.STARFORGE_INGOT.get());
                        output.accept(ModItems.STARFORGE_BLOCK.get());
                        output.accept(ModItems.STARFORGE_SWORD.get());
                        output.accept(ModItems.STARFORGE_PICKAXE.get());
                        output.accept(ModItems.STARFORGE_AXE.get());
                        output.accept(ModItems.STARFORGE_SHOVEL.get());
                        output.accept(ModItems.STARFORGE_HOE.get());
                        output.accept(ModItems.STARFORGE_HELMET.get());
                        output.accept(ModItems.STARFORGE_CHESTPLATE.get());
                        output.accept(ModItems.STARFORGE_LEGGINGS.get());
                        output.accept(ModItems.STARFORGE_BOOTS.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
