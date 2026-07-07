/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import com.deltaops.DeltaOpsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 倉庫系統管理器：
 * 每個玩家擁有 54 格（9x6）的倉庫，儲存於 PersistentData NBT 中。
 * 提供儲存、讀取、賣出等功能。
 */
public class WarehouseManager {

    private static final String WAREHOUSE_TAG = DeltaOpsMod.MOD_ID + ":warehouse";
    private static final String SLOTS_TAG = "slots";
    private static final int WAREHOUSE_SIZE = 54; // 9x6

    /** 取得玩家的倉庫 ItemStackHandler */
    public static ItemStackHandler getWarehouse(ServerPlayer player) {
        ItemStackHandler handler = new ItemStackHandler(WAREHOUSE_SIZE);
        CompoundTag data = player.getPersistentData().getCompound(WAREHOUSE_TAG);
        if (data.contains(SLOTS_TAG)) {
            ListTag list = data.getList(SLOTS_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && i < WAREHOUSE_SIZE; i++) {
                handler.setStackInSlot(i, ItemStack.of(list.getCompound(i)));
            }
        }
        return handler;
    }

    /** 儲存玩家的倉庫 */
    public static void saveWarehouse(ServerPlayer player, ItemStackHandler handler) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < handler.getSlots(); i++) {
            CompoundTag slotTag = new CompoundTag();
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.save(slotTag);
            }
            list.add(slotTag);
        }
        data.put(SLOTS_TAG, list);
        player.getPersistentData().put(WAREHOUSE_TAG, data);
    }

    /** 計算倉庫所有物品的總價值 */
    public static long calculateWarehouseValue(ServerPlayer player) {
        ItemStackHandler handler = getWarehouse(player);
        long total = 0L;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                total += com.deltaops.lobby.EconomyManager.getItemPrice(stack) * stack.getCount();
            }
        }
        return total;
    }

    /** 賣出倉庫中指定槽位的物品（賣出後槽位清空） */
    public static long sellItem(ServerPlayer player, int slot) {
        ItemStackHandler handler = getWarehouse(player);
        if (slot < 0 || slot >= handler.getSlots()) return 0L;
        ItemStack stack = handler.getStackInSlot(slot);
        if (stack.isEmpty()) return 0L;
        long price = com.deltaops.lobby.EconomyManager.getItemPrice(stack) * stack.getCount();
        if (price > 0L) {
            com.deltaops.lobby.EconomyManager.addBalance(player, price);
        }
        handler.setStackInSlot(slot, ItemStack.EMPTY);
        saveWarehouse(player, handler);
        return price;
    }

    /** 賣出倉庫中所有物品 */
    public static long sellAll(ServerPlayer player) {
        ItemStackHandler handler = getWarehouse(player);
        long total = 0L;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                long price = com.deltaops.lobby.EconomyManager.getItemPrice(stack) * stack.getCount();
                if (price > 0L) {
                    com.deltaops.lobby.EconomyManager.addBalance(player, price);
                    total += price;
                }
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        saveWarehouse(player, handler);
        return total;
    }
}
