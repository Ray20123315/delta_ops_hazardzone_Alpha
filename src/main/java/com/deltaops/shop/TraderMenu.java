/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.shop;

import com.deltaops.lobby.EconomyManager;
import com.deltaops.screen.ModMenuTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class TraderMenu extends AbstractContainerMenu {
    private final ItemStackHandler shopInventory = new ItemStackHandler(18);
    private final ServerPlayer player;

    public TraderMenu(int id, Inventory inventory) {
        super(ModMenuTypes.TRADER.get(), id);
        this.player = (ServerPlayer) inventory.player;

        // 從 TraderData 載入商人商品
        java.util.List<TraderData.TraderItem> items = TraderData.getItems();
        for (int i = 0; i < Math.min(items.size(), 18); i++) {
            TraderData.TraderItem ti = items.get(i);
            ItemStack stack = ti.itemStack().copy();
            stack.setCount(ti.stackSize());
            shopInventory.setStackInSlot(i, stack);
        }

        // 商人商品欄位（唯讀，不可取走）
        int startX = 8;
        int startY = 18;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new SlotItemHandler(shopInventory, index, startX + col * 18, startY + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // 玩家背包欄位
        int playerStartY = startY + 2 * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, row * 9 + col + 9, startX + col * 18, playerStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, startX + col * 18, playerStartY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * 玩家點擊商人商品欄位時，由外部（screen）呼叫此方法進行購買。
     */
    public boolean purchase(int shopSlotIndex) {
        if (shopSlotIndex < 0 || shopSlotIndex >= shopInventory.getSlots()) return false;
        ItemStack stack = shopInventory.getStackInSlot(shopSlotIndex);
        if (stack.isEmpty()) return false;

        java.util.List<TraderData.TraderItem> items = TraderData.getItems();
        if (shopSlotIndex >= items.size()) return false;
        TraderData.TraderItem ti = items.get(shopSlotIndex);
        long price = ti.price();

        // 檢查餘額
        long balance = EconomyManager.getBalance(player);
        if (balance < price) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c餘額不足！需要 " + price + " 哈夫幣（目前: " + balance + "）。"));
            return false;
        }

        // 安全箱購買（特殊處理）
        if (ti.isSecureBox()) {
            int targetLevel = ti.secureBoxLevel();
            int currentLevel = com.deltaops.securebox.SecureBoxCapabilityManager.getSecureBoxLevel(player);
            if (targetLevel <= currentLevel && com.deltaops.securebox.SecureBoxCapabilityManager.isUnlocked(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e你已擁有等級 " + currentLevel + " 的安全箱，無需重複購買。"));
                return false;
            }
            boolean success = com.deltaops.securebox.SecureBoxCapabilityManager.purchase(player, targetLevel);
            return success;
        }

        // 一般商品
        ItemStack product = ti.itemStack().copy();
        product.setCount(ti.stackSize());
        if (!player.getInventory().add(product)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c背包已滿！"));
            return false;
        }

        // 扣款
        EconomyManager.addBalance(player, -price);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a購買成功！花費 " + price + " 哈夫幣。"));
        return true;
    }
}
