/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.combat;

import com.deltaops.DeltaOpsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WeaponConfigScreen extends AbstractContainerScreen<WeaponConfigMenu> {
    private static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");

    public WeaponConfigScreen(WeaponConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        guiGraphics.fill(x, y, x + w, y + h, 0xFF171B23);
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF222A37);
        guiGraphics.fill(x + 6, y + 6, x + w - 6, y + h - 6, 0xFF303A4A);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "§6§l🔫 槍械設定", this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "§7放入物品=新增 / 取出=移除", this.leftPos + 8, this.topPos + this.imageHeight - 94, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.leftPos + 8, this.topPos + this.imageHeight - 82, 0xFFE0E7FF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
