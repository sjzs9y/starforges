package com.starforge.client;

import com.starforge.network.TogglePayload;
import com.starforge.whisper.WhisperAbility;
import com.starforge.whisper.WhisperData;
import com.starforge.whisper.WhisperItem;
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
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_STEP = 22;
    /** 每列最多放几行；超过就自动分成多列，避免界面越加越长。 */
    private static final int MAX_ROWS = 8;
    private static final int COLUMN_GAP = 6;

    private final Map<WhisperAbility, Button> buttons = new EnumMap<>(WhisperAbility.class);

    private int gridTop;

    public WhisperScreen() {
        super(Component.translatable("screen.starforge.whisper"));
    }

    @Override
    protected void init() {
        this.buttons.clear();

        WhisperAbility[] abilities = WhisperAbility.all();
        int count = abilities.length;

        // 自动分列：超过 MAX_ROWS 行就再开一列
        int columns = (count + MAX_ROWS - 1) / MAX_ROWS;
        int rows = (count + columns - 1) / columns;

        int buttonWidth = Math.min(PANEL_WIDTH, (this.width - 40 - (columns - 1) * COLUMN_GAP) / columns);
        int totalWidth = columns * buttonWidth + (columns - 1) * COLUMN_GAP;
        int totalHeight = (rows - 1) * ROW_STEP + BUTTON_HEIGHT;

        int left = (this.width - totalWidth) / 2;
        int top = (this.height - totalHeight) / 2 + 10;
        this.gridTop = top;

        for (int i = 0; i < count; i++) {
            WhisperAbility ability = abilities[i];
            int column = i / rows;
            int row = i % rows;
            int x = left + column * (buttonWidth + COLUMN_GAP);
            int y = top + row * ROW_STEP;

            Button button = Button.builder(label(ability), b -> toggle(ability))
                    .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                    .build();
            this.buttons.put(ability, button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int titleY = Math.max(10, this.gridTop - 18);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFFFFF);
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
        if (newValue) {
            WhisperAbility exclusive = ability.getExclusive();
            if (exclusive != null) {
                WhisperData.setEnabled(stack, exclusive, false);
            }
        }
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
