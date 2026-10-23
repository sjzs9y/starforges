package com.starforge.registry;

import com.starforge.Starforge;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All blocks of the mod. The matching {@code BlockItem} is registered in {@link ModItems}.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Starforge.MOD_ID);

    public static final DeferredBlock<Block> STARFORGE_BLOCK = BLOCKS.registerSimpleBlock(
            "starforge_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    private ModBlocks() {
    }
}
