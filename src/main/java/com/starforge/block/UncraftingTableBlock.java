package com.starforge.block;

import com.starforge.uncrafting.UncraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 流光拆解台：右键打开拆解界面。
 *
 * <p>界面里放一个物品，右侧 3×3 会显示能把它拆回哪些材料；
 * 点击任意一个产物会把整组材料一起取出，并消耗一份原物品。</p>
 */
public class UncraftingTableBlock extends Block {

    private static final Component TITLE = Component.translatable("container.starforge.uncrafting_table");

    public UncraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hitResult) {
        openMenu(level, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        openMenu(level, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void openMenu(Level level, Player player) {
        if (level.isClientSide()) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new UncraftingMenu(containerId, inventory),
                TITLE));
    }
}
