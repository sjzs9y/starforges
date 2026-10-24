package com.starforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.starforge.network.CycleModePayload;
import com.starforge.whisper.WhisperItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only glue: the "G" (open menu) and "Z" (switch right-click mode) key bindings.
 *
 * <p>This class is only touched when {@code FMLEnvironment.dist.isClient()}, so a dedicated server
 * never loads it.</p>
 */
public final class ClientSetup {

    /** 71 is GLFW_KEY_G. The number is used directly to avoid depending on the LWJGL classes. */
    private static final int KEY_G = 71;
    /** 90 is GLFW_KEY_Z. */
    private static final int KEY_Z = 90;

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.starforge.open_whisper",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            KEY_G,
            "key.categories.starforge");

    public static final KeyMapping SWITCH_MODE = new KeyMapping(
            "key.starforge.switch_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            KEY_Z,
            "key.categories.starforge");

    /** Called once during mod construction, client side only. */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
    }

    /** Called through the mod event bus, client side only. */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
        event.register(SWITCH_MODE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        while (OPEN_MENU.consumeClick()) {
            if (minecraft.player == null || minecraft.screen != null) {
                continue;
            }
            if (minecraft.player.getMainHandItem().getItem() instanceof WhisperItem) {
                minecraft.setScreen(new WhisperScreen());
            }
        }

        while (SWITCH_MODE.consumeClick()) {
            if (minecraft.player == null || minecraft.screen != null) {
                continue;
            }
            if (minecraft.player.getMainHandItem().getItem() instanceof WhisperItem) {
                PacketDistributor.sendToServer(new CycleModePayload());
            }
        }
    }

    private ClientSetup() {
    }
}
