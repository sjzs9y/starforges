package com.starforge.whisper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side behaviour of the Whisper item.
 *
 * <p>被动能力（无敌 / 飞行 / 夜视 / 饱和 / 水下呼吸 / 真隐身）只要物品在背包、副手或主手任意位置
 * 就会生效；主动能力（一击必杀）仍然要求拿在主手。</p>
 *
 * <p>所有效果都每 tick 从物品重新判定，没有隐藏状态：把物品丢出去或者关掉开关，效果立刻停止。</p>
 */
public final class WhisperEvents {

    /** How long the continuously refreshed effects last. Slightly more than 1 second to be safe. */
    private static final int EFFECT_DURATION = 220;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = WhisperData.findWhisper(player);
        if (stack.isEmpty()) {
            updateFlight(player, false);
            return;
        }

        updateFlight(player, isOn(stack, WhisperAbility.FLIGHT));

        if (isOn(stack, WhisperAbility.NIGHT_VISION)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false));
        }
        if (isOn(stack, WhisperAbility.SATURATION)) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false));
        }
        if (isOn(stack, WhisperAbility.WATER_BREATHING)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_DURATION, 0, false, false));
        }
        if (isOn(stack, WhisperAbility.TRUE_INVISIBILITY)) {
            // 视觉隐身；「怪不主动锁定你」由 onLivingChangeTarget 处理
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_DURATION, 0, false, false));
        }
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 无敌（被动）：只要身上带着并且开关打开，就完全免疫伤害
        if (event.getEntity() instanceof ServerPlayer player
                && WhisperData.isPassiveEnabled(player, WhisperAbility.INVINCIBLE)) {
            event.setCanceled(true);
            return;
        }

        // 一击必杀（主动）：手持攻击时把伤害抬到必死
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof WhisperItem && WhisperData.isEnabled(stack, WhisperAbility.ONE_HIT)) {
                event.setAmount(1000.0F);
            }
        }
    }

    /**
     * 真隐身：怪物不会把开启该功能的玩家设为目标（已经锁定过的会在下一次 AI 重选目标时放弃）。
     */
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player
                && WhisperData.isPassiveEnabled(player, WhisperAbility.TRUE_INVISIBILITY)) {
            event.setCanceled(true);
        }
    }

    private static boolean isOn(ItemStack stack, WhisperAbility ability) {
        return WhisperData.isEnabled(stack, ability);
    }

    /**
     * Gives or removes the "may fly" ability. Creative and spectator players keep their own flight.
     */
    private static void updateFlight(ServerPlayer player, boolean enabled) {
        boolean wanted = enabled || player.isCreative() || player.isSpectator();
        if (player.getAbilities().mayfly != wanted) {
            player.getAbilities().mayfly = wanted;
            if (!wanted) {
                player.getAbilities().flying = false;
            }
            player.onUpdateAbilities();
        }
    }

    private WhisperEvents() {
    }
}
