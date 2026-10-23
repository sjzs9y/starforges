package com.starforge.whisper;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Reads and writes the on/off switches of the Whisper item.
 *
 * <p>The switches live in the item stack's {@code minecraft:custom_data} component, which is part
 * of the stack's data component map and is therefore automatically synchronised to the client.
 * That is why the GUI can read the current state without an extra request.</p>
 */
public final class WhisperData {

    private static final String ROOT = "Whisper";

    public static boolean isEnabled(ItemStack stack, WhisperAbility ability) {
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

    private WhisperData() {
    }
}
