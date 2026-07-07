/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 賣出 Menu：左邊是倉庫物品、右邊是賣出區
 */
public class SellMenu extends AbstractContainerMenu {

    private final ItemStackHandler warehouse;
    private final ItemStackHandler sellArea;
    private final Player player;

    public SellMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, inv.player);
    }

    public SellMenu(int id, Inventory inv, Player player) {
        super(ModInventoryMenus.SELL.get(), id);
        this.player = player;
        this.sellArea = new ItemStackHandler(9); // 賣出區 9 格

        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            this.warehouse = WarehouseManager.getWarehouse(sp);
        } else {
            this.warehouse = new ItemStackHandler(54);
        }

        // 倉庫欄位 (54格: 6x9，左側)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                int idx = row * 9 + col;
                if (idx < 54) {
                    addSlot(new SlotItemHandler(warehouse, idx, 8 + col * 18, 18 + row * 18));
                }
            }
        }

        // 賣出區欄位 (右側 3x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(sellArea, row * 3 + col, 134 + col * 18, 36 + row * 18));
            }
        }

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 198));
        }
    }

    /** 確認賣出：計算賣出區物品價格，加入餘額，清空賣出區 */
    public long confirmSell() {
        long total = 0L;
        for (int i = 0; i < sellArea.getSlots(); i++) {
            ItemStack stack = sellArea.getStackInSlot(i);
            if (!stack.isEmpty()) {
                long price = com.deltaops.lobby.EconomyManager.getItemPrice(stack) * stack.getCount();
                if (price > 0L && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    com.deltaops.lobby.EconomyManager.addBalance(sp, price);
                    total += price;
                }
                sellArea.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        // 儲存倉庫變更
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            WarehouseManager.saveWarehouse(sp, warehouse);
        }
        return total;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // 倉庫 → 賣出區（優先）或背包
        if (index < 54) {
            if (!moveItemStackTo(stack, 54, 63, false)) {
                if (!moveItemStackTo(stack, 63, 99, false)) return ItemStack.EMPTY;
            }
        }
        // 賣出區 → 倉庫
        else if (index < 63) {
            if (!moveItemStackTo(stack, 0, 54, false)) return ItemStack.EMPTY;
        }
        // 背包 → 倉庫
        else {
            if (!moveItemStackTo(stack, 0, 54, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 賣出區未確認的物品退回倉庫
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            ItemStackHandler wh = WarehouseManager.getWarehouse(sp);
            for (int i = 0; i < sellArea.getSlots(); i++) {
                ItemStack stack = sellArea.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    for (int j = 0; j < wh.getSlots(); j++) {
                        stack = wh.insertItem(j, stack, false);
                        if (stack.isEmpty()) break;
                    }
                }
            }
            WarehouseManager.saveWarehouse(sp, wh);
        }
    }
}
