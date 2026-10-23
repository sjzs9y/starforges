package com.starforge.whisper;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 轻语轻扬 — a plain item that only carries the ability switches.
 *
 * <p>All of the actual behaviour is server side, see {@link WhisperEvents}.</p>
 */
public class WhisperItem extends Item {

    public WhisperItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.starforge.whisper.hint").withStyle(ChatFormatting.GRAY));
    }
}
