package com.starforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 流光影像：手里拿着什么，右键就复制出 64 个什么。
 *
 * <p>交互写在 {@link #useItemOn} 和 {@link #useWithoutItem} 两个入口里，
 * 所以不管手里有没有物品、也不管轻语轻扬有没有抢交互，最终都会走这里的复制逻辑。</p>
 *
 * <p>复制的是主手物品的完整 {@link ItemStack}（保留自定义数据/耐久），
 * 但每次只复制 1 个、共 {@link #DUPLICATE_COUNT} 个；能堆叠的物品会合并成整组掉落，
 * 避免一次生成 64 个掉落物实体。</p>
 */
public class FlowingImageBlock extends Block {

    /** 一次右键复制多少个。 */
    public static final int DUPLICATE_COUNT = 64;

    public FlowingImageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hitResult) {
        duplicateHeldItem(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        duplicateHeldItem(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void duplicateHeldItem(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }

        int remaining = DUPLICATE_COUNT;
        while (remaining > 0) {
            int count = Math.min(remaining, Math.max(1, held.getMaxStackSize()));
            Block.popResource(level, pos, held.copyWithCount(count));
            remaining -= count;
        }
    }
}
