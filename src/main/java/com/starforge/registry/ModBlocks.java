package com.starforge.registry;

import com.starforge.Starforge;
import com.starforge.block.FlowingImageBlock;
import com.starforge.block.UncraftingTableBlock;
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

    /** 流光影像：右键复制手里物品 64 个，外观暂用黑色混凝土。 */
    public static final DeferredBlock<FlowingImageBlock> FLOWING_IMAGE = BLOCKS.register(
            "flowing_image",
            () -> new FlowingImageBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.8F)
                    .sound(SoundType.STONE)));

    /** 流光拆解台：右键打开拆解界面。 */
    public static final DeferredBlock<UncraftingTableBlock> UNCRAFTING_TABLE = BLOCKS.register(
            "uncrafting_table",
            () -> new UncraftingTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));

    private ModBlocks() {
    }
}
