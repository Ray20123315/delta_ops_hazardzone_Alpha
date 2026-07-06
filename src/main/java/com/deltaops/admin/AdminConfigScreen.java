package com.deltaops.admin;

import com.deltaops.lobby.HazardMapRegistry;
import com.deltaops.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class AdminConfigScreen extends AbstractContainerScreen<AdminConfigMenu> {
    private int selectedMapIndex = 0;
    private final List<String> mapIds = new ArrayList<>(HazardMapRegistry.getAllMaps().keySet());
    private EditBox itemIdBox;
    private EditBox priceBox;

    // 分頁系統
    private int currentPage = 0;
    private static final int MAX_PAGES = 3;
    private Button prevButton;
    private Button nextButton;
    private Button saveButton;
    private Button cancelButton;

    public AdminConfigScreen(AdminConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 300;
        this.imageHeight = 220;
    }

    private void drawPanelBackground(GuiGraphics guiGraphics) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        guiGraphics.fill(x, y, x + w, y + h, 0xFF171B23);
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF222A37);
        guiGraphics.fill(x + 6, y + 6, x + w - 6, y + h - 6, 0xFF313C4D);
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(360, Math.max(280, this.width - 32));
        this.imageHeight = Math.min(240, Math.max(200, this.height - 32));
        super.init();

        int cx = this.leftPos + this.imageWidth / 2;
        int bw = Math.min(120, this.imageWidth - 40);

        // ===== 底部導航按鈕（固定位置） =====
        int navY = this.topPos + this.imageHeight - 28;

        this.prevButton = this.addRenderableWidget(Button.builder(
                Component.literal("◀ 上一頁"), btn -> {
            if (currentPage > 0) currentPage--;
            rebuildPage(cx, bw);
        }).bounds(this.leftPos + 8, navY, 60, 20).build());

        this.nextButton = this.addRenderableWidget(Button.builder(
                Component.literal("下一頁 ▶"), btn -> {
            if (currentPage < MAX_PAGES - 1) currentPage++;
            rebuildPage(cx, bw);
        }).bounds(this.leftPos + this.imageWidth - 68, navY, 60, 20).build());

        this.saveButton = this.addRenderableWidget(Button.builder(
                Component.literal("§a✔ 儲存"), btn -> {
            // 全部設定已即時送出，此按鈕為關閉
            Minecraft.getInstance().setScreen(null);
        }).bounds(cx - bw / 2 - 35, navY, 70, 20).build());

        this.cancelButton = this.addRenderableWidget(Button.builder(
                Component.literal("§c✖ 取消"), btn -> {
            Minecraft.getInstance().setScreen(null);
        }).bounds(cx + 35, navY, 70, 20).build());

        rebuildPage(cx, bw);
    }

    protected void clearWidgets() {
        // 只清除非導航按鈕的部分
        this.renderables.clear();
        this.children().clear();
        // 重新加入導航按鈕
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(cancelButton);

        // 重新 init 按鈕
        int cx = this.leftPos + this.imageWidth / 2;
        int bw = Math.min(120, this.imageWidth - 40);
        int navY = this.topPos + this.imageHeight - 28;
        prevButton.setPosition(this.leftPos + 8, navY);
        nextButton.setPosition(this.leftPos + this.imageWidth - 68, navY);
        saveButton.setPosition(cx - bw / 2 - 35, navY);
        cancelButton.setPosition(cx + 35, navY);
    }

    private void rebuildPage(int cx, int bw) {
        clearWidgets();

        int baseX = this.leftPos + 12;
        int pageLabelY = this.topPos + 18;

        switch (currentPage) {
            case 0 -> buildPageMap(baseX, pageLabelY, bw);
            case 1 -> buildPageEconomy(baseX, pageLabelY, bw);
            case 2 -> buildPageRules(baseX, pageLabelY, bw);
        }
    }

    // ===== 第 1 頁：地圖設定 =====
    private void buildPageMap(int baseX, int topY, int bw) {
        int y = topY;

        this.addRenderableWidget(Button.builder(Component.literal("◀ " + getMapName(-1)), btn -> {
            this.selectedMapIndex = (this.selectedMapIndex + this.mapIds.size() - 1) % this.mapIds.size();
            rebuildPage(this.leftPos + this.imageWidth / 2, bw);
        }).bounds(baseX, y, 70, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(getMapName(0) + " ▶"), btn -> {
            this.selectedMapIndex = (this.selectedMapIndex + 1) % this.mapIds.size();
            rebuildPage(this.leftPos + this.imageWidth / 2, bw);
        }).bounds(baseX + 74, y, 70, 20).build());

        String currentMap = this.mapIds.isEmpty() ? "無地圖" : this.mapIds.get(this.selectedMapIndex);
        this.addRenderableWidget(Button.builder(Component.literal("📍 設置撤離點"), btn -> {
            String mapId = this.mapIds.get(this.selectedMapIndex);
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.SET_EXTRACTION_POINT, mapId));
        }).bounds(baseX, y + 28, bw, 20).build());
    }

    private String getMapName(int offset) {
        if (mapIds.isEmpty()) return "無";
        int idx = offset == 0 ? selectedMapIndex : (selectedMapIndex + offset + mapIds.size()) % mapIds.size();
        String id = mapIds.get(idx);
        var def = HazardMapRegistry.getMap(id);
        return def != null ? def.displayName() : id;
    }

    // ===== 第 2 頁：經濟設定 =====
    private void buildPageEconomy(int baseX, int topY, int bw) {
        int y = topY;

        this.addRenderableWidget(Button.builder(Component.literal("🔄 重載物價表"), btn -> {
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.RELOAD_PRICES, ""));
        }).bounds(baseX, y, bw, 20).build());

        int editY = y + 28;
        this.itemIdBox = this.addRenderableWidget(new EditBox(this.font, baseX, editY, Math.min(90, bw - 56), 18, Component.literal("物品 id")));
        this.itemIdBox.setMaxLength(128);
        this.itemIdBox.setHint(Component.literal("minecraft:diamond"));

        this.priceBox = this.addRenderableWidget(new EditBox(this.font, baseX + Math.min(90, bw - 56) + 4, editY, 48, 18, Component.literal("價格")));
        this.priceBox.setMaxLength(16);
        this.priceBox.setHint(Component.literal("1800"));

        this.addRenderableWidget(Button.builder(Component.literal("💾 保存價格"), btn -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String itemId = this.itemIdBox.getValue();
                String priceText = this.priceBox.getValue();
                try {
                    long price = Long.parseLong(priceText);
                    ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                            ServerboundAdminConfigActionPacket.Action.SET_ITEM_PRICE, "", itemId, price));
                    mc.player.sendSystemMessage(Component.literal("已更新物品價格：" + itemId + " = " + price));
                } catch (NumberFormatException ignored) {
                    mc.player.sendSystemMessage(Component.literal("價格必須是數字。"));
                }
            }
        }).bounds(baseX, editY + 22, bw, 20).build());

        // 獎勵倍率
        this.addRenderableWidget(Button.builder(Component.literal("⭐ 倍率: " + this.rewardMultiplier + "x"), btn -> {
            double[] options = {0.5, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0};
            this.multiplierIndex = (this.multiplierIndex + 1) % options.length;
            this.rewardMultiplier = options[this.multiplierIndex];
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.SET_REWARD_MULTIPLIER, "", "", (long)(this.rewardMultiplier * 100)));
        }).bounds(baseX, editY + 48, bw, 20).build());
    }

    // ===== 第 3 頁：遊戲規則 =====
    private void buildPageRules(int baseX, int topY, int bw) {
        int y = topY;

        // 安全箱等級（唯讀顯示，不能在此切換，需前往商店購買）
        this.addRenderableWidget(Button.builder(Component.literal("🔒 安全箱: Lv" + this.secureBoxLevel + " §7(前往商店購買)"), btn -> {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§e請使用 /dt shop 前往商店購買安全箱解鎖。"));
        }).bounds(baseX, y, bw, 20).build());

        // 死亡掉落規則
        this.addRenderableWidget(Button.builder(Component.literal("💀 掉落: " + getDropLabel()), btn -> {
            String[] rules = {"ALL", "INVENTORY_ONLY", "NONE"};
            this.deathDropIndex = (this.deathDropIndex + 1) % rules.length;
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.SET_DEATH_DROP_RULE, rules[this.deathDropIndex]));
        }).bounds(baseX, y + 28, bw, 20).build());

        // 撤離倒數
        this.addRenderableWidget(Button.builder(Component.literal("⏱ 撤離: " + this.extractionTimer + "s"), btn -> {
            int[] options = {5, 10, 15, 20, 30, 60, 120};
            this.extractionTimerIndex = (this.extractionTimerIndex + 1) % options.length;
            this.extractionTimer = options[this.extractionTimerIndex];
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.SET_EXTRACTION_TIMER, "", "", this.extractionTimer));
        }).bounds(baseX, y + 56, bw, 20).build());
    }

    private String getDropLabel() {
        return switch (this.deathDropIndex) {
            case 0 -> "全部掉落";
            case 1 -> "僅背包";
            case 2 -> "不掉落";
            default -> "全部掉落";
        };
    }

    private int secureBoxLevel = 1;
    private int deathDropIndex = 0;
    private int extractionTimer = 10;
    private int extractionTimerIndex = 1;
    private double rewardMultiplier = 1.0;
    private int multiplierIndex = 1;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        drawPanelBackground(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String[] pageTitles = {"📍 地圖設定", "💰 經濟設定", "⚙ 規則設定"};
        String title = pageTitles[currentPage] + "  (" + (currentPage + 1) + "/" + MAX_PAGES + ")";
        guiGraphics.drawString(this.font, title, this.leftPos + 12, this.topPos + 6, 0xFFFFFF, false);
    }
}
