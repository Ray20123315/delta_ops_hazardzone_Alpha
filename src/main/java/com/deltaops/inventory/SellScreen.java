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
 * 賣出畫面：左邊倉庫、右邊賣出區、下方確認按鈕
 */
public class SellScreen extends AbstractContainerScreen<SellMenu> {

    private static final ResourceLocation HAF_COIN = new ResourceLocation(DeltaOpsMod.MOD_ID, "textures/gui/haf_coin.png");

    public SellScreen(SellMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 230;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        // 確認賣出按鈕
        addRenderableWidget(Button.builder(
                Component.literal("§a§l確認賣出"),
                btn -> {
                    long total = menu.confirmSell();
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.sendSystemMessage(Component.literal("§a賣出完成，獲得 " + total + " 哈夫幣！"));
                    }
                }
        ).bounds((width - 120) / 2, height - 40, 120, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.fill(0, 0, width, height, 0xCC000000);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 倉庫區 (左半)
        gui.fill(x, y, x + 120, y + 130, 0xFF2B2B2B);
        gui.drawString(font, Component.literal("§6倉庫"), x + 8, y + 5, 0xFFFFFF);

        // 賣出區 (右半)
        gui.fill(x + 126, y, x + imageWidth, y + 100, 0xFF3A2A2A);
        gui.drawString(font, Component.literal("§e賣出區"), x + 134, y + 5, 0xFFAA00);

        // 分隔線
        gui.fill(x + 122, y, x + 124, y + 130, 0xFF555555);
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
