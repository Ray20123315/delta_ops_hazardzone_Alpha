/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.shop;

import com.deltaops.DeltaOpsMod;
import com.deltaops.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TraderScreen extends AbstractContainerScreen<TraderMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DeltaOpsMod.MOD_ID, "textures/gui/trader.png");

    public TraderScreen(TraderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 繪製面板背景
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
        guiGraphics.drawString(this.font, "§6§l商人", this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.leftPos + 8, this.topPos + this.imageHeight - 94, 0xFFE0E7FF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 滑鼠懸停在商品上時顯示價格
        for (int i = 0; i < 18; i++) {
            var slot = this.menu.slots.get(i);
            if (!slot.hasItem()) continue;
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                java.util.List<TraderData.TraderItem> items = TraderData.getItems();
                if (i < items.size()) {
                    TraderData.TraderItem ti = items.get(i);
                    String desc;
                    if (ti.isSecureBox()) {
                        desc = "§5📦 安全箱 Lv" + ti.secureBoxLevel() + "\n§e價格: " + ti.price() + " 哈夫幣\n§7購買後永久解鎖對應等級\n§7按 O 鍵開啟";
                    } else {
                        desc = "§e價格: " + ti.price() + " 哈夫幣\n§7點擊購買";
                    }
                    guiGraphics.renderTooltip(this.font, net.minecraft.network.chat.Component.literal(desc), mouseX, mouseY);
                }
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 點擊商品欄位觸發購買
        for (int i = 0; i < 18; i++) {
            var slot = this.menu.slots.get(i);
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                if (slot.hasItem()) {
                    // 通知伺服端購買
                    ModNetwork.CHANNEL.sendToServer(new ServerboundPurchasePacket(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
