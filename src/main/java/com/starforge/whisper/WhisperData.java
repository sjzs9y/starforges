package com.starforge.whisper;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Reads and writes the on/off switches of the Whisper item, and locates the item in a player's
 * inventory.
 *
 * <p>The switches live in the item stack's {@code minecraft:custom_data} component, which is part
 * of the stack's data component map and is therefore automatically synchronised to the client.
 * That is why the GUI can read the current state without an extra request.</p>
 */
public final class WhisperData {

    private static final String ROOT = "Whisper";

    public static boolean isEnabled(ItemStack stack, WhisperAbility ability) {
        if (!(stack.getItem() instanceof WhisperItem)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(ROOT)) {
            return false;
        }
        return tag.getCompound(ROOT).getBoolean(ability.getKey());
    }

    public static void setEnabled(ItemStack stack, WhisperAbility ability, boolean value) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        CompoundTag root = tag.getCompound(ROOT);
        root.putBoolean(ability.getKey(), value);
        tag.put(ROOT, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void toggle(ItemStack stack, WhisperAbility ability) {
        setEnabled(stack, ability, !isEnabled(stack, ability));
    }

    /**
     * 找出玩家身上的轻语轻扬：主手 → 副手 → 背包 36 格。
     *
     * <p>注意 {@code getInventory().items} 只有主背包那 36 格，不含副手，所以副手要单独判断。</p>
     *
     * @return 找到的物品栈，没找到返回 {@link ItemStack#EMPTY}
     */
    public static ItemStack findWhisper(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof WhisperItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof WhisperItem) {
            return off;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof WhisperItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 玩家身上带着的物品里，某个被动开关是否处于打开状态。 */
    public static boolean isPassiveEnabled(Player player, WhisperAbility ability) {
        return isEnabled(findWhisper(player), ability);
    }

    private WhisperData() {
    }
}
