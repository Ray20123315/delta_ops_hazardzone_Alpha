/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.loot;

import com.deltaops.DeltaOpsMod;
import com.deltaops.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AdminItemTaggingScreen extends AbstractContainerScreen<AdminItemTaggingMenu> {
    private LootCategory selectedCategory = LootCategory.COLLECTIBLE;
    private ItemQuality selectedQuality = ItemQuality.WHITE;

    // 3 頁：0=選取物品, 1=選擇類別, 2=選擇品質
    private int currentPage = 0;
    private static final int MAX_PAGES = 3;
    private Button prevButton;
    private Button nextButton;
    private Button saveButton;
    private Button cancelButton;

    public AdminItemTaggingScreen(AdminItemTaggingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 280;
        this.imageHeight = 220;
    }

    private void drawPanelBackground(GuiGraphics guiGraphics) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        guiGraphics.fill(x, y, x + w, y + h, 0xFF171B23);
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF222A37);
        guiGraphics.fill(x + 6, y + 6, x + w - 6, y + h - 6, 0xFF303A4A);
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(340, Math.max(260, this.width - 40));
        this.imageHeight = Math.min(240, Math.max(210, this.height - 40));
        super.init();

        int cx = this.leftPos + this.imageWidth / 2;
        int navY = this.topPos + this.imageHeight - 28;

        this.prevButton = this.addRenderableWidget(Button.builder(
                Component.literal("◀ 上一頁"), btn -> {
            if (currentPage > 0) currentPage--;
            rebuildPage();
        }).bounds(this.leftPos + 8, navY, 60, 20).build());

        this.nextButton = this.addRenderableWidget(Button.builder(
                Component.literal("下一頁 ▶"), btn -> {
            if (currentPage < MAX_PAGES - 1) currentPage++;
            rebuildPage();
        }).bounds(this.leftPos + this.imageWidth - 68, navY, 60, 20).build());

        this.saveButton = this.addRenderableWidget(Button.builder(
                Component.literal("§a✔ 批次儲存"), btn -> doBatchSave()
        ).bounds(cx - 35, navY, 70, 20).build());

        this.cancelButton = this.addRenderableWidget(Button.builder(
                Component.literal("§c✖ 取消"), btn -> {
            Minecraft.getInstance().setScreen(null);
        }).bounds(cx + 35, navY, 70, 20).build());

        rebuildPage();
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children().clear();
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(cancelButton);

        int cx = this.leftPos + this.imageWidth / 2;
        int navY = this.topPos + this.imageHeight - 28;
        prevButton.setPosition(this.leftPos + 8, navY);
        nextButton.setPosition(this.leftPos + this.imageWidth - 68, navY);
        saveButton.setPosition(cx - 35, navY);
        cancelButton.setPosition(cx + 35, navY);
    }

    private void rebuildPage() {
        clearWidgets();
        int baseX = this.leftPos + 12;
        int y = this.topPos + 20;

        switch (currentPage) {
            case 0 -> {
                // 第 1 頁：提示放入物品到容器
                // 容器本身由 AbstractContainerScreen 自動繪製
            }
            case 1 -> buildCategoryPage(baseX, y);
            case 2 -> buildQualityPage(baseX, y);
        }
    }

    private void buildCategoryPage(int baseX, int y) {
        LootCategory[] cats = LootCategory.values();
        int cols = 3;
        int btnW = Math.min(100, (this.imageWidth - 40) / cols);
        for (int i = 0; i < cats.length; i++) {
            int col = i % cols;
            int row = i / cols;
            LootCategory cat = cats[i];
            boolean isSelected = cat == selectedCategory;
            String label = (isSelected ? "§6▶ " : "  ") + cat.name();
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                selectedCategory = cat;
                rebuildPage();
            }).bounds(baseX + col * (btnW + 4), y + row * 24, btnW, 20).build());
        }
        // 顯示已選取物品數量
        int count = countSelectedItems();
        this.addRenderableWidget(Button.builder(
                Component.literal("§7已選 " + count + " 個物品"), btn -> {}
        ).bounds(baseX, y + 80, btnW * 2 + 10, 20).build());
    }

    private void buildQualityPage(int baseX, int y) {
        ItemQuality[] quals = ItemQuality.values();
        int cols = 3;
        int btnW = Math.min(100, (this.imageWidth - 40) / cols);
        for (int i = 0; i < quals.length; i++) {
            int col = i % cols;
            int row = i / cols;
            ItemQuality q = quals[i];
            boolean isSelected = q == selectedQuality;
            String color = switch (q) {
                case WHITE -> "§f";
                case GREEN -> "§a";
                case BLUE -> "§9";
                case PURPLE -> "§5";
                case GOLD -> "§6";
                case RED -> "§c";
            };
            String label = (isSelected ? "§6▶ " : "  ") + color + q.name();
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                selectedQuality = q;
                rebuildPage();
            }).bounds(baseX + col * (btnW + 4), y + row * 24, btnW, 20).build());
        }
        // 顯示已選取物品數量
        int count = countSelectedItems();
        this.addRenderableWidget(Button.builder(
                Component.literal("§7已選 " + count + " 個物品"), btn -> {}
        ).bounds(baseX, y + 80, btnW * 2 + 10, 20).build());
    }

    private int countSelectedItems() {
        ItemStackHandler handler = this.menu.getSelectedItems();
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) count++;
        }
        return count;
    }

    private void doBatchSave() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStackHandler handler = this.menu.getSelectedItems();
        List<String> toSave = new ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null) {
                toSave.add(id.toString());
            }
        }

        if (toSave.isEmpty()) {
            mc.player.sendSystemMessage(Component.literal("§c尚未選取任何物品！請先在第1頁將物品放入上方的格子中。"));
            return;
        }

        for (String itemId : toSave) {
            ModNetwork.CHANNEL.sendToServer(new ServerboundSaveItemTagPacket(itemId, selectedCategory, selectedQuality));
        }

        mc.player.sendSystemMessage(Component.literal("§a已批次儲存 " + toSave.size() + " 個物品標籤："
                + selectedCategory.name() + " / " + selectedQuality.name()));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawPanelBackground(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String[] titles = {"📦 選取物品", "📂 選取類別", "🏅 選取品質"};
        guiGraphics.drawString(this.font, titles[currentPage] + "  (" + (currentPage + 1) + "/" + MAX_PAGES + ")",
                this.leftPos + 12, this.topPos + 8, 0xFFFFFF, false);

        if (currentPage == 0) {
            guiGraphics.drawString(this.font, Component.literal("§7將物品放入上方容器=選取，取出=取消"),
                    this.leftPos + 12, this.topPos + this.imageHeight - 40, 0xAAAAAA, false);
        }

        guiGraphics.drawString(this.font, Component.literal("§e當前設定: " + selectedCategory.name() + " / " + selectedQuality.name()),
                this.leftPos + 12, this.topPos + this.imageHeight - 52, 0xFFFFAA, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
