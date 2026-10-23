package com.starforge.client;

import com.starforge.whisper.WhisperAbility;
import com.starforge.whisper.WhisperData;
import com.starforge.whisper.WhisperItem;
import com.starforge.network.TogglePayload;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The "轻语轻扬" menu: one plain toggle button per ability.
 *
 * <p>The buttons read their state straight from the held item stack, which already contains the
 * synced custom data, so opening the screen needs no extra round trip.</p>
 */
public class WhisperScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int ROW_HEIGHT = 24;

    private final Map<WhisperAbility, Button> buttons = new EnumMap<>(WhisperAbility.class);

    public WhisperScreen() {
        super(Component.translatable("screen.starforge.whisper"));
    }

    @Override
    protected void init() {
        this.buttons.clear();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = this.height / 2 - 60;

        WhisperAbility[] abilities = WhisperAbility.all();
        for (int i = 0; i < abilities.length; i++) {
            WhisperAbility ability = abilities[i];
            Button button = Button.builder(label(ability), b -> toggle(ability))
                    .bounds(left, top + i * ROW_HEIGHT, PANEL_WIDTH, 20)
                    .build();
            this.buttons.put(ability, button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 80, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void toggle(WhisperAbility ability) {
        ItemStack stack = heldItem();
        if (stack == null) {
            return;
        }
        boolean newValue = !WhisperData.isEnabled(stack, ability);
        // optimistic client update so the label reacts instantly
        WhisperData.setEnabled(stack, ability, newValue);
        PacketDistributor.sendToServer(new TogglePayload(ability.ordinal(), newValue));
        refreshLabels();
    }

    private void refreshLabels() {
        this.buttons.forEach((ability, button) -> button.setMessage(label(ability)));
    }

    private Component label(WhisperAbility ability) {
        ItemStack stack = heldItem();
        boolean on = stack != null && WhisperData.isEnabled(stack, ability);
        Component state = Component.translatable(on ? "gui.starforge.on" : "gui.starforge.off");
        return Component.translatable(ability.getTranslationKey()).append(": ").append(state);
    }

    private ItemStack heldItem() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        return stack.getItem() instanceof WhisperItem ? stack : null;
    }
}
