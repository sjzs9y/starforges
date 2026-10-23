package com.starforge.whisper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side behaviour of the Whisper item.
 *
 * <p>Everything is re-evaluated every tick from the item stack in the main hand, so there is no
 * hidden state that can get out of sync: drop the item (or switch the ability off) and the effect
 * stops immediately.</p>
 */
public final class WhisperEvents {

    /** How long the continuously refreshed effects last. Slightly more than 1 second to be safe. */
    private static final int EFFECT_DURATION = 220;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WhisperItem)) {
            updateFlight(player, false);
            return;
        }

        updateFlight(player, WhisperData.isEnabled(stack, WhisperAbility.FLIGHT));

        if (WhisperData.isEnabled(stack, WhisperAbility.NIGHT_VISION)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false));
        }
        if (WhisperData.isEnabled(stack, WhisperAbility.SATURATION)) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false));
        }
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        // 无敌: the holder takes no damage at all
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof WhisperItem && WhisperData.isEnabled(stack, WhisperAbility.INVINCIBLE)) {
                event.setCanceled(true);
                return;
            }
        }

        // 一击必杀: any hit with the item does lethal damage
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof WhisperItem && WhisperData.isEnabled(stack, WhisperAbility.ONE_HIT)) {
                event.setAmount(1000.0F);
            }
        }
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
