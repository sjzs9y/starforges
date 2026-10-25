package com.starforge.whisper;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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

    // ------------------------------------------------------------------ 右键模式

    private static final String MODE_KEY = "Mode";

    public static WhisperMode getMode(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return WhisperMode.ATTACK;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(ROOT)) {
            return WhisperMode.ATTACK;
        }
        return WhisperMode.byKey(tag.getCompound(ROOT).getString(MODE_KEY));
    }

    public static void setMode(ItemStack stack, WhisperMode mode) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        CompoundTag root = tag.getCompound(ROOT);
        root.putString(MODE_KEY, mode.getKey());
        tag.put(ROOT, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Z 键：切到下一个模式，并自动打开该模式对应的总开关。
     *
     * <p>之前只切模式、不碰开关，新给的轻语默认所有主动开关都是关的，
     * 所以会出现「模式显示复制，但右键没反应」的假故障。</p>
     */
    public static WhisperMode cycleMode(ItemStack stack) {
        WhisperMode next = getMode(stack).next();
        setMode(stack, next);
        setEnabled(stack, next.getRequiredToggle(), true);
        return next;
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

    /**
     * 这个生物是否「持有」轻语轻扬。
     *
     * <p>玩家查主手 / 副手 / 背包；其它生物只查装备栏（主手、副手、头盔、胸甲、护腿、靴子），
     * 不查它们的容器背包（马箱、狐狸叼着的东西之类）。</p>
     */
    public static boolean holdsWhisper(LivingEntity entity) {
        if (entity instanceof Player player) {
            return !findWhisper(player).isEmpty();
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (entity.getItemBySlot(slot).getItem() instanceof WhisperItem) {
                return true;
            }
        }
        return false;
    }

    /**
     * 秒杀白名单：创造模式玩家、旁观模式玩家、以及任何持有轻语轻扬的生物。
     *
     * <p>命中白名单时一击必杀不生效，改为固定伤害；范围攻击同理。</p>
     */
    public static boolean isProtectedTarget(LivingEntity target) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        return holdsWhisper(target);
    }

    private WhisperData() {
    }
}
