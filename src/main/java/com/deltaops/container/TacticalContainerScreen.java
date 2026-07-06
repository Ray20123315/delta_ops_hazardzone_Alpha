/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.container;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TacticalContainerScreen extends AbstractContainerScreen<TacticalContainerMenu> {
    public TacticalContainerScreen(TacticalContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = Math.max(176, 24 + menu.getVariant().getWidth() * 18 + 24);
        this.imageHeight = Math.max(166, 24 + menu.getVariant().getHeight() * 18 + 96);
    }

    private void drawPanelBackground(GuiGraphics guiGraphics, int panelX, int panelY, int panelWidth, int panelHeight) {
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF10131A);
        guiGraphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, 0xFF1A2130);
        guiGraphics.fill(panelX + 6, panelY + 6, panelX + panelWidth - 6, panelY + panelHeight - 6, 0xFF232B3B);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();

        int panelX = this.leftPos;
        int panelY = this.topPos;
        int panelWidth = this.imageWidth;
        int panelHeight = this.imageHeight;

        drawPanelBackground(guiGraphics, panelX, panelY, panelWidth, panelHeight);

        int gridStartX = panelX + 8;
        int gridStartY = panelY + 20;
        int cellSize = 18;
        int width = this.menu.getVariant().getWidth();
        int height = this.menu.getVariant().getHeight();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int x = gridStartX + col * cellSize;
                int y = gridStartY + row * cellSize;
                guiGraphics.fill(x, y, x + 16, y + 16, 0xFF3C4453);
                guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF4E5A6B);
            }
        }

        if (this.menu.isHiddenLayerUnlocked() && this.menu.getVariant().supportsHiddenLayer()) {
            int hiddenX = panelX + 8 + width * 18 + 16;
            int hiddenY = panelY + 20;
            for (int index = 0; index < 3; index++) {
                guiGraphics.fill(hiddenX, hiddenY + index * 18, hiddenX + 16, hiddenY + index * 18 + 16, 0xFF6A3C90);
                guiGraphics.fill(hiddenX + 1, hiddenY + index * 18 + 1, hiddenX + 15, hiddenY + index * 18 + 15, 0xFF8E5CD8);
            }
        }

        guiGraphics.drawString(this.font, this.title, panelX + 8, panelY + 8, 0xFFF7C948, false);
        guiGraphics.drawString(this.font, "隱藏夾層", panelX + 8 + width * 18 + 16, panelY + 8, 0xFFD9C4FF, false);

        RenderSystem.disableBlend();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.leftPos + 8, this.topPos + this.imageHeight - 94, 0xFFE5E7EB, false);
    }
}
