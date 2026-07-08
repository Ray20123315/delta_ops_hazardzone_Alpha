package com.deltaops.admin;

import com.deltaops.lobby.EconomyManager;
import com.deltaops.lobby.HazardMapRegistry;
import com.deltaops.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AdminConfigScreen extends AbstractContainerScreen<AdminConfigMenu> {
    private int selectedMapIndex = 0;
    private final List<String> mapIds = new ArrayList<>(HazardMapRegistry.getAllMaps().keySet());
    private EditBox priceBox;

    // 分頁系統
    private int currentPage = 0;
    private static final int MAX_PAGES = 4; // 新增第4頁
    private Button prevButton;
    private Button nextButton;
    private Button saveButton;
    private Button cancelButton;

    // 價格編輯用
    private int selectedPriceSlot = -1; // -1 = 未選取
    private static final int[] EXTRACTION_TIMER_OPTIONS = {5, 10, 15, 20, 30, 60, 120};
    private static final double[] MULTIPLIER_OPTIONS = {0.5, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0};

    public AdminConfigScreen(AdminConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 300;
        this.imageHeight = 230;
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
        this.imageHeight = Math.min(250, Math.max(220, this.height - 32));
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
            Minecraft.getInstance().setScreen(null);
        }).bounds(cx - bw / 2 - 35, navY, 70, 20).build());

        this.cancelButton = this.addRenderableWidget(Button.builder(
                Component.literal("§c✖ 離開"), btn -> {
            Minecraft.getInstance().setScreen(null);
        }).bounds(cx + 35, navY, 70, 20).build());

        rebuildPage(cx, bw);
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children().clear();
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(cancelButton);

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
            case 3 -> buildPagePriceEdit(baseX, pageLabelY, bw);
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

    // ===== 第 2 頁：經濟設定（重整後移至第4頁，此頁改為快捷按鈕） =====
    private void buildPageEconomy(int baseX, int topY, int bw) {
        int y = topY;

        this.addRenderableWidget(Button.builder(Component.literal("🔄 重載物價表"), btn -> {
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.RELOAD_PRICES, ""));
        }).bounds(baseX, y, bw, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("📦 物品價格編輯 §7(第4頁)"), btn -> {
            currentPage = 3;
            rebuildPage(this.leftPos + this.imageWidth / 2, bw);
        }).bounds(baseX, y + 28, bw, 20).build());

        // 獎勵倍率
        this.addRenderableWidget(Button.builder(Component.literal("⭐ 倍率: " + this.rewardMultiplier + "x"), btn -> {
            this.multiplierIndex = (this.multiplierIndex + 1) % MULTIPLIER_OPTIONS.length;
            this.rewardMultiplier = MULTIPLIER_OPTIONS[this.multiplierIndex];
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.SET_REWARD_MULTIPLIER, "", "", (long)(this.rewardMultiplier * 100)));
        }).bounds(baseX, y + 56, bw, 20).build());

        // 快捷按鈕：前往商店設定安全箱等級
        this.addRenderableWidget(Button.builder(Component.literal("🔒 安全箱等級"), btn -> {
            String[] levels = {"1", "2", "3", "4"};
            int current = (int)((System.currentTimeMillis() / 1000) % 4); // 簡單循環
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§e請使用 /dt shop 前往商店購買安全箱解鎖。"));
        }).bounds(baseX, y + 84, bw, 20).build());
    }

    // ===== 第 3 頁：遊戲規則 =====
    private void buildPageRules(int baseX, int topY, int bw) {
        int y = topY;

        this.addRenderableWidget(Button.builder(Component.literal("🔒 安全箱: Lv" + this.secureBoxLevel), btn -> {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§e請使用 /dt shop 前往商店購買安全箱解鎖。"));
        }).bounds(baseX, y, bw, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("💀 掉落: " + getDropLabel()), btn -> {
            String[] rules = {"ALL", "INVENTORY_ONLY", "NONE"};
            this.deathDropIndex = (this.deathDropIndex + 1) % rules.length;
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.SET_DEATH_DROP_RULE, rules[this.deathDropIndex]));
        }).bounds(baseX, y + 28, bw, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("⏱ 撤離: " + this.extractionTimer + "s"), btn -> {
            this.extractionTimerIndex = (this.extractionTimerIndex + 1) % EXTRACTION_TIMER_OPTIONS.length;
            this.extractionTimer = EXTRACTION_TIMER_OPTIONS[this.extractionTimerIndex];
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.SET_EXTRACTION_TIMER, "", "", this.extractionTimer));
        }).bounds(baseX, y + 56, bw, 20).build());
    }

    /** 精簡版價格編輯資訊（供 renderLabels 繪製） */
    private String priceEditHint = "";
    private String priceEditSelected = "";
    private String[] slotPriceLabels = new String[6];

    // ===== 第 4 頁（NEW）：鐵砧風格物品價格編輯 =====
    private void buildPagePriceEdit(int baseX, int topY, int bw) {
        int y = topY;

        // 上方提示（由 renderLabels 繪製）
        priceEditHint = "§e§l✎ 鐵砧改名 — 放入物品，輸入數字設定價格 (限 OP)";
        y += 14;

        // 初始化價格標籤
        for (int col = 0; col < 6; col++) {
            ItemStack stack = menu.priceSlots.getStackInSlot(col);
            if (!stack.isEmpty()) {
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                long currentPrice = com.deltaops.lobby.EconomyManager.getItemPrice(itemId);
                slotPriceLabels[col] = currentPrice > 0 ? "§a$" + currentPrice : "";
            } else {
                slotPriceLabels[col] = "";
            }
        }

        y = this.topPos + 55;

        // 價格輸入區（鐵砧風格）
        if (this.priceBox == null) {
            this.priceBox = new EditBox(this.font, baseX + 2, y + 2, bw - 4, 16, Component.literal("價格"));
            this.priceBox.setMaxLength(16);
            this.priceBox.setHint(Component.literal("§7✎ 輸入數字作為價格..."));
        }
        this.priceBox.setX(baseX + 2);
        this.priceBox.setY(y + 2);
        this.priceBox.setWidth(bw - 4);
        this.addRenderableWidget(priceBox);

        y += 26;

        // 選取物品提示（由 renderLabels 繪製）
        if (selectedPriceSlot >= 0 && selectedPriceSlot < 6) {
            ItemStack selStack = menu.priceSlots.getStackInSlot(selectedPriceSlot);
            if (!selStack.isEmpty()) {
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(selStack.getItem()).toString();
                long currentPrice = com.deltaops.lobby.EconomyManager.getItemPrice(itemId);
                String itemName = selStack.getHoverName().getString();
                priceEditSelected = "§e已選取: §f" + itemName + " §7(" + itemId + ")"
                        + (currentPrice > 0 ? " §a目前價格: $" + currentPrice : " §7(無價格)");
            } else {
                priceEditSelected = "§c第 " + (selectedPriceSlot + 1) + " 格為空，請放入物品";
            }
        } else {
            priceEditSelected = "§7點擊上方物品欄位選取要設定價格的物品";
        }

        // 操作按鈕
        this.addRenderableWidget(Button.builder(Component.literal("💾 儲存價格"), btn -> {
            doSavePrice();
        }).bounds(baseX, y, bw / 2 - 4, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("🔄 重新載入"), btn -> {
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.RELOAD_PRICES, ""));
        }).bounds(baseX + bw / 2 + 4, y, bw / 2 - 4, 20).build());

        y += 24;

        this.addRenderableWidget(Button.builder(Component.literal("§a✔ 批次儲存全部"), btn -> {
            doBatchSaveAllPrices();
        }).bounds(baseX, y, bw / 2 - 4, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c✖ 清空欄位"), btn -> {
            clearAllPriceSlots();
        }).bounds(baseX + bw / 2 + 4, y, bw / 2 - 4, 20).build());

        // 加入底部欄位操作說明
    }

    /** 儲存單一物品價格 */
    private void doSavePrice() {
        if (selectedPriceSlot < 0 || selectedPriceSlot >= 6) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c請先選取一個物品欄位。"));
            return;
        }
        ItemStack stack = menu.priceSlots.getStackInSlot(selectedPriceSlot);
        if (stack.isEmpty()) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c該欄位沒有物品，請先放入物品。"));
            return;
        }
        String priceText = this.priceBox.getValue().trim();
        if (priceText.isEmpty()) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c請輸入價格數字。"));
            return;
        }
        try {
            long price = Long.parseLong(priceText);
            if (price < 0 || price > 999999999) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c價格範圍：0 ~ 999,999,999"));
                return;
            }
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                    ServerboundAdminConfigActionPacket.Action.SET_ITEM_PRICE, "", itemId, price));
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(
                    "§a已設定 §f" + stack.getHoverName().getString() + " §a價格為 §6$" + price));
        } catch (NumberFormatException e) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c價格必須是數字！"));
        }
    }

    /** 批次儲存所有非空欄位的物品價格 */
    private void doBatchSaveAllPrices() {
        String priceText = this.priceBox.getValue().trim();
        if (priceText.isEmpty()) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c請先輸入一個價格數字，將套用到所有非空欄位。"));
            return;
        }
        try {
            long price = Long.parseLong(priceText);
            if (price < 0 || price > 999999999) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c價格範圍：0 ~ 999,999,999"));
                return;
            }
            int count = 0;
            for (int i = 0; i < 6; i++) {
                ItemStack stack = menu.priceSlots.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    ModNetwork.CHANNEL.sendToServer(new ServerboundAdminConfigActionPacket(
                            ServerboundAdminConfigActionPacket.Action.SET_ITEM_PRICE, "", itemId, price));
                    count++;
                }
            }
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(
                    "§a已批次設定 §f" + count + " §a個物品價格為 §6$" + price));
        } catch (NumberFormatException e) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c價格必須是數字！"));
        }
    }

    /** 清空所有價格設定欄位 */
    private void clearAllPriceSlots() {
        for (int i = 0; i < 6; i++) {
            menu.priceSlots.setStackInSlot(i, ItemStack.EMPTY);
        }
        selectedPriceSlot = -1;
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("§a已清空所有價格設定欄位。"));
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

        // 第 4 頁：繪製價格編輯欄位的 slot 邊框與物品
        if (currentPage == 3) {
            for (int col = 0; col < 6; col++) {
                int slotX = this.leftPos + 26 + col * 22;
                int slotY = this.topPos + 30;

                // slot 邊框（選取時高亮）
                int borderColor = selectedPriceSlot == col ? 0x80FFFF55 : 0xFF444444;
                guiGraphics.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, borderColor);

                // 物品圖示
                ItemStack stack = menu.priceSlots.getStackInSlot(col);
                if (!stack.isEmpty()) {
                    guiGraphics.renderItem(stack, slotX, slotY);
                    guiGraphics.renderItemDecorations(this.font, stack, slotX, slotY);

                    // 價格標籤
                    if (!slotPriceLabels[col].isEmpty()) {
                        guiGraphics.drawString(this.font, slotPriceLabels[col], slotX, slotY - 10, 0xFFFFFF, false);
                    }
                }
            }

            // 鐵砧輸入區背景
            int baseX = this.leftPos + 12;
            int bw = Math.min(120, this.imageWidth - 40);
            int iby = this.topPos + 55;
            guiGraphics.fill(baseX - 2, iby - 2, baseX + bw + 2, iby + 22, 0xFF555555);
            guiGraphics.fill(baseX, iby, baseX + bw, iby + 20, 0xFF1A1A2E);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String[] pageTitles = {
                "📍 地圖設定",
                "💰 經濟設定",
                "⚙ 規則設定",
                "✎ 鐵砧價格編輯"
        };
        String title = pageTitles[currentPage] + "  (" + (currentPage + 1) + "/" + MAX_PAGES + ")";
        guiGraphics.drawString(this.font, title, 12, 6, 0xFFFFFF, false);

        // 第 4 頁：繪製提示與選取資訊（使用相對座標）
        if (currentPage == 3) {
            guiGraphics.drawString(this.font, priceEditHint, 12, 18, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, priceEditSelected, 12, 83, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 處理 slot 點擊選取
        for (int col = 0; col < 6; col++) {
            int slotX = this.leftPos + 26 + col * 22;
            int slotY = this.topPos + 30;
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                this.selectedPriceSlot = col;
                // 更新 EditBox 顯示該物品目前價格
                ItemStack stack = menu.priceSlots.getStackInSlot(col);
                if (!stack.isEmpty()) {
                    String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    long currentPrice = com.deltaops.lobby.EconomyManager.getItemPrice(itemId);
                    if (currentPrice > 0) {
                        this.priceBox.setValue(String.valueOf(currentPrice));
                    } else {
                        this.priceBox.setValue("");
                        this.priceBox.setHint(Component.literal("§e輸入 " + stack.getHoverName().getString() + " 的價格"));
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.priceBox != null && this.priceBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter
                doSavePrice();
                return true;
            }
            return this.priceBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
