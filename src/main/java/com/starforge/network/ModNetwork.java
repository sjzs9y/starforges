package com.starforge.network;

import com.starforge.whisper.WhisperAbility;
import com.starforge.whisper.WhisperData;
import com.starforge.whisper.WhisperItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the mod's network payloads.
 */
public final class ModNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TogglePayload.TYPE, TogglePayload.STREAM_CODEC, ModNetwork::handleToggle);
    }

    private static void handleToggle(TogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof WhisperItem)) {
                return;
            }

            WhisperAbility ability = WhisperAbility.byIndex(payload.ability());

            // 精准模式 / 幸运模式互斥：打开一个就把另一个关掉
            if (payload.value()) {
                WhisperAbility exclusive = ability.getExclusive();
                if (exclusive != null) {
                    WhisperData.setEnabled(stack, exclusive, false);
                }
            }

            WhisperData.setEnabled(stack, ability, payload.value());

            // push the new custom data to the client so the GUI shows the real state
            player.inventoryMenu.broadcastChanges();
        });
    }

    private ModNetwork() {
    }
}
