package com.starforge.client;

import com.starforge.network.CycleUncraftPayload;
import com.starforge.uncrafting.UncraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 拆解台界面。
 *
 * <p>不依赖额外贴图：背景、槽位和按钮全部用 {@link GuiGraphics#fill} 画出来，
 * 这样即使之后换美术资源，也不会因为缺少 png 导致界面空白。</p>
 */
public class UncraftingScreen extends AbstractContainerScreen<UncraftingMenu> {

    public UncraftingScreen(UncraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> PacketDistributor.sendToServer(new CycleUncraftPayload(-1)))
                .bounds(this.leftPos + 64, this.topPos + 35, 16, 16)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> PacketDistributor.sendToServer(new CycleUncraftPayload(1)))
                .bounds(this.leftPos + 64, this.topPos + 53, 16, 16)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        graphics.fill(x, y, x + this.imageWidth, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + this.imageHeight, 0xFFFFFFFF);
        graphics.fill(x + 1, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF555555);
        graphics.fill(x + this.imageWidth - 1, y + 1, x + this.imageWidth, y + this.imageHeight, 0xFF555555);

        drawSlot(graphics, 36, 35);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                drawSlot(graphics, 94 + column * 18, 17 + row * 18);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, 8 + column * 18, 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, 8 + column * 18, 142);
        }

        graphics.drawString(this.font, "→", x + 82, y + 39, 0xFF404040, false);
    }

    private void drawSlot(GuiGraphics graphics, int slotX, int slotY) {
        int x = this.leftPos + slotX - 1;
        int y = this.topPos + slotY - 1;
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title.getString(), this.titleLabelX, this.titleLabelY,
                0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle.getString(), this.inventoryLabelX,
                this.inventoryLabelY, 0xFF404040, false);
    }
}
