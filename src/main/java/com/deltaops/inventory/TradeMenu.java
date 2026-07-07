/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 交易 Menu：雙方各有 18 格（2x9）展示區
 */
public class TradeMenu extends AbstractContainerMenu {

    private final ItemStackHandler myOffers;
    private final SharedTradeHandler handler;
    private final boolean isSecondary;

    public TradeMenu(int id, Inventory inv, FriendlyByteBuf data) {
        this(id, inv, inv.player, null, false);
    }

    public TradeMenu(int id, Inventory inv, Player player, SharedTradeHandler handler, boolean isSecondary) {
        super(ModInventoryMenus.TRADE.get(), id);
        this.handler = handler;
        this.isSecondary = isSecondary;
        this.myOffers = new ItemStackHandler(18);

        if (handler != null) {
            handler.setMenus(this, id, inv, isSecondary);
        }

        // 我的物品區 (2x9 = 18格)
        for (int i = 0; i < 18; i++) {
            addSlot(new SlotItemHandler(myOffers, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }

        // 對方物品區 (唯讀，僅顯示)
        // (畫面中不加入 Slot，因為是對方的物品，由 Screen 自行渲染)
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // 從我的物品區移到背包
        if (index < 18) {
            if (!moveItemStackTo(stack, 18, 54, false)) return ItemStack.EMPTY;
        }
        // 從背包移到我的物品區
        else if (index >= 18 && index < 54) {
            if (!moveItemStackTo(stack, 0, 18, false)) return ItemStack.EMPTY;
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
        // 未完成的交易物品退回背包
        for (int i = 0; i < myOffers.getSlots(); i++) {
            ItemStack stack = myOffers.getStackInSlot(i);
            if (!stack.isEmpty() && player instanceof ServerPlayer sp) {
                sp.getInventory().add(stack);
            }
        }
    }

    /**
     * 確認交易（雙方都確認後執行）
     */
    public boolean confirmTrade() {
        if (handler == null) return false;
        return handler.confirm(this, isSecondary);
    }

    /**
     * 共享交易處理器：同步雙方放入的物品
     */
    public static class SharedTradeHandler {
        public final ServerPlayer p1, p2;
        private TradeMenu menu1, menu2;
        private boolean confirmed1, confirmed2;

        public SharedTradeHandler(ServerPlayer p1, ServerPlayer p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        public void setMenus(TradeMenu menu, int id, Inventory inv, boolean secondary) {
            if (secondary) this.menu2 = menu;
            else this.menu1 = menu;
        }

        public synchronized boolean confirm(TradeMenu menu, boolean secondary) {
            if (secondary) confirmed2 = true;
            else confirmed1 = true;

            if (confirmed1 && confirmed2) {
                // 雙方都確認，交換物品
                executeTrade();
                return true;
            }
            return false; // 等待對方確認
        }

        private void executeTrade() {
            // P1 → P2
            for (int i = 0; i < 18; i++) {
                ItemStack stack = menu1.myOffers.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    p2.getInventory().add(stack);
                    menu1.myOffers.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            // P2 → P1
            for (int i = 0; i < 18; i++) {
                ItemStack stack = menu2.myOffers.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    p1.getInventory().add(stack);
                    menu2.myOffers.setStackInSlot(i, ItemStack.EMPTY);
                }
            }

            p1.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a交易成功！"));
            p2.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a交易成功！"));

            // 關閉雙方選單
            p1.closeContainer();
            p2.closeContainer();
        }
    }
}
