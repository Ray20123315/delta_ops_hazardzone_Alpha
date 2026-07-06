/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.loot;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 物品標籤編輯器的 ContainerMenu。
 * 上方 18 格為選取的物品清單（放入即選取、取出即取消選取）。
 * 下方為玩家背包。
 */
public class AdminItemTaggingMenu extends AbstractContainerMenu {
    private final ItemStackHandler selectedItems = new ItemStackHandler(18);
    public LootCategory selectedCategory = LootCategory.COLLECTIBLE;
    public ItemQuality selectedQuality = ItemQuality.WHITE;

    public AdminItemTaggingMenu(int id, Inventory inventory) {
        super(null, id);

        // 選取物品欄位（2 行 × 9 格）
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new SlotItemHandler(selectedItems, index, 8 + col * 18, 36 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !stack.isEmpty();
                    }
                });
            }
        }

        // 玩家背包欄位
        int playerStartY = 36 + 2 * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, playerStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, playerStartY + 58));
        }
    }

    public ItemStackHandler getSelectedItems() {
        return selectedItems;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        if (index < 18) {
            // 從選取區取出 = 取消選取
            if (!this.moveItemStackTo(stack, 18, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 從背包放入選取區 = 選取物品
            if (!this.moveItemStackTo(stack, 0, 18, false)) {
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
