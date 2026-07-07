/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 倉庫 Menu：54 格倉庫 + 36 格玩家背包
 */
public class WarehouseMenu extends AbstractContainerMenu {

    private final ItemStackHandler warehouse;
    private final Player player;

    // 客戶端建構（由 IForgeMenuType 工廠呼叫）
    public WarehouseMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, inv.player);
    }

    // 伺服端建構
    public WarehouseMenu(int id, Inventory inv, Player player) {
        super(ModInventoryMenus.WAREHOUSE.get(), id);
        this.player = player;
        this.warehouse = player instanceof net.minecraft.server.level.ServerPlayer sp
                ? WarehouseManager.getWarehouse(sp)
                : new ItemStackHandler(54);

        // 倉庫欄位 (54格: 9x6)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(warehouse, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }

        // 玩家背包欄位 (27格)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        // 玩家快捷欄 (9格)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 198));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // 從倉庫移到背包
        if (index < 54) {
            if (!moveItemStackTo(stack, 54, 90, false)) return ItemStack.EMPTY;
        }
        // 從背包移到倉庫
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
        // 儲存倉庫
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            WarehouseManager.saveWarehouse(sp, warehouse);
        }
    }
}
