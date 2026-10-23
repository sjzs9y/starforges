package com.starforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.starforge.whisper.WhisperItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only glue: the "G" key binding and the client tick hook that opens the menu.
 *
 * <p>This class is only touched when {@code FMLEnvironment.dist.isClient()}, so a dedicated server
 * never loads it.</p>
 */
public final class ClientSetup {

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.starforge.open_whisper",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.starforge");

    /** Called once during mod construction, client side only. */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
    }

    /** Called through the mod event bus, client side only. */
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
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
    }

    private ClientSetup() {
    }
}
