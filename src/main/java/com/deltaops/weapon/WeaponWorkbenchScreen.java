/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.weapon;

import com.deltaops.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

public class WeaponWorkbenchScreen extends AbstractContainerScreen<WeaponWorkbenchMenu> {
    public WeaponWorkbenchScreen(WeaponWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    protected void init() {
        super.init();

        int cx = this.leftPos + this.imageWidth / 2;
        int cy = this.topPos + 58;

        this.addRenderableWidget(Button.builder(Component.literal("安裝"), btn -> {
            ModNetwork.CHANNEL.sendToServer(new ServerboundWeaponWorkbenchPacket(ServerboundWeaponWorkbenchPacket.Action.ATTACH));
        }).bounds(cx - 80, cy, 50, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("卸載"), btn -> {
            ModNetwork.CHANNEL.sendToServer(new ServerboundWeaponWorkbenchPacket(ServerboundWeaponWorkbenchPacket.Action.DETACH_ALL));
        }).bounds(cx - 20, cy, 50, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("返回"), btn -> this.onClose()).bounds(cx + 40, cy, 50, 20).build());
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
        guiGraphics.drawString(this.font, "§6§l武器改裝檯", this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);

        // 顯示已安裝配件
        guiGraphics.drawString(this.font, "武器", this.leftPos + 24, this.topPos + 24, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, "配件", this.leftPos + 60, this.topPos + 24, 0xAAAAAA, false);

        int infoY = this.topPos + 72;
        guiGraphics.drawString(this.font, "§7已安裝配件:", this.leftPos + 8, infoY, 0xFFFFFF, false);
        Map<WeaponAttachment.AttachmentType, String> atts = WeaponAttachment.getAllAttachments(this.menu.getWeapon());
        int line = 1;
        for (Map.Entry<WeaponAttachment.AttachmentType, String> entry : atts.entrySet()) {
            WeaponAttachment.AttachmentDef def = WeaponAttachment.getAttachmentDef(entry.getValue());
            String name = def != null ? def.displayName() : entry.getValue();
            guiGraphics.drawString(this.font, " §7" + entry.getKey().getTagName() + ": §f" + name,
                    this.leftPos + 12, infoY + line * 10, 0xCCCCCC, false);
            line++;
        }
        if (atts.isEmpty()) {
            guiGraphics.drawString(this.font, " §7無", this.leftPos + 12, infoY + 10, 0x888888, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
