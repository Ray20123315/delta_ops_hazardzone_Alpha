/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import com.deltaops.DeltaOpsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 交易畫面
 */
public class TradeScreen extends AbstractContainerScreen<TradeMenu> {

    public TradeScreen(TradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        // 確認交易按鈕
        addRenderableWidget(Button.builder(
                Component.literal("§a§l確認交易"),
                btn -> {
                    boolean done = menu.confirmTrade();
                    if (done) {
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("§a交易完成！"));
                        }
                    } else {
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.sendSystemMessage(Component.literal("§e已確認，等待對方確認..."));
                        }
                    }
                }
        ).bounds((width - 100) / 2, height - 35, 100, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.fill(0, 0, width, height, 0xCC000000);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 我的物品區
        gui.fill(x, y, x + imageWidth, y + 56, 0xFF2B2B2B);
        gui.drawString(font, Component.literal("§6我的物品"), x + 8, y + 5, 0xFFFFFF);

        // 背包
        gui.fill(x, y + 56, x + imageWidth, y + imageHeight, 0xFF1A1A1A);
        gui.drawString(font, Component.literal("§7背包"), x + 8, y + 58, 0xAAAAAA);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);
        renderTooltip(gui, mouseX, mouseY);
    }
}
