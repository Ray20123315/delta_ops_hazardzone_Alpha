/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 武器設定容器 Menu：
 * 玩家把物品放入此容器 → 自動加入 WeaponConfig 槍械清單
 * 從此容器取出物品 → 自動從 WeaponConfig 移除
 */
public class WeaponConfigMenu extends AbstractContainerMenu {
    private final ItemStackHandler configInventory = new ItemStackHandler(18) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
        }
    };
    private final ServerPlayer player;

    public WeaponConfigMenu(int id, Inventory inventory) {
        super(null, id);
        this.player = (ServerPlayer) inventory.player;

        // 載入已設定的物品（唯讀顯示用，實際操作透過 insert/extract）
        int idx = 0;
        for (String registryName : WeaponConfig.getAllWeaponIds()) {
            if (idx >= 18) break;
            String rn = registryName;
            int slot = idx;
            net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getOptional(new net.minecraft.resources.ResourceLocation(rn))
                    .ifPresent(item -> configInventory.setStackInSlot(slot, new ItemStack(item)));
            idx++;
        }

        // 設定容器欄位（18 格，2 行）
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new SlotItemHandler(configInventory, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return true;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !stack.isEmpty();
                    }

                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        // 取出時從 WeaponConfig 移除
                        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                        if (id != null) {
                            WeaponConfig.removeWeapon(id.toString());
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§c已移除: " + id.toString()));
                        }
                        super.onTake(player, stack);
                    }
                });
            }
        }

        // 玩家背包欄位
        int playerStartY = 18 + 2 * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, row * 9 + col + 9, 8 + col * 18, playerStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, playerStartY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();

        // 判斷是從設定區（0~17）移出還是從背包（18+）移入
        if (index < 18) {
            // 從設定區移出 = 移除設定
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) {
                WeaponConfig.removeWeapon(id.toString());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c已移除: " + id.toString()));
            }
            if (!this.moveItemStackTo(stack, 18, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 從背包移入設定區 = 新增設定
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) {
                boolean added = WeaponConfig.addWeapon(stack);
                if (added) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a已新增: " + id.toString()));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e已是槍械: " + id.toString()));
                }
            }
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
