/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import com.deltaops.DeltaOpsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 倉庫畫面：9x6 網格，顯示倉庫+背包
 */
public class WarehouseScreen extends AbstractContainerScreen<WarehouseMenu> {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(DeltaOpsMod.MOD_ID, "textures/gui/warehouse.png");
    private static final ResourceLocation HAF_COIN = new ResourceLocation(DeltaOpsMod.MOD_ID, "textures/gui/haf_coin.png");

    public WarehouseScreen(WarehouseMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        // 深色背景
        gui.fill(0, 0, width, height, 0xCC000000);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 倉庫區域背景
        gui.fill(x, y, x + imageWidth, y + 130, 0xFF2B2B2B);
        // 背包區域背景
        gui.fill(x, y + 130, x + imageWidth, y + imageHeight, 0xFF1A1A1A);

        // 標籤文字
        gui.drawString(font, Component.literal("§6倉庫 (9x6)"), x + 8, y + 5, 0xFFFFFF);
        gui.drawString(font, Component.literal("§7背包"), x + 8, y + 128, 0xAAAAAA);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 不畫預設標題（已自繪）
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);
        renderTooltip(gui, mouseX, mouseY);
    }
}
