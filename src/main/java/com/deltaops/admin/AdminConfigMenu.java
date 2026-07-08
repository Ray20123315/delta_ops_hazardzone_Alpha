package com.deltaops.admin;

import com.deltaops.screen.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 管理員設定 GUI 的 ContainerMenu。
 * 上方 6 格為價格設定用物品欄位（管理員放入物品後可設定價格）。
 * 下方為玩家背包。
 */
public class AdminConfigMenu extends AbstractContainerMenu {
    /** 6 格價格設定用物品欄位 */
    public final ItemStackHandler priceSlots = new ItemStackHandler(6);

    public AdminConfigMenu(int id, Inventory inventory) {
        super(ModMenuTypes.ADMIN_CONFIG.get(), id);

        // 價格設定物品欄位（1 行 × 6 格）
        for (int col = 0; col < 6; col++) {
            this.addSlot(new SlotItemHandler(priceSlots, col, 26 + col * 22, 30) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !stack.isEmpty();
                }
            });
        }

        // 玩家背包欄位（3 行）
        int playerStartY = 60;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, playerStartY + row * 18));
            }
        }
        // 玩家快捷欄（1 行）
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, playerStartY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        if (index < 6) {
            // 從價格設定區取出 = 移回背包
            if (!this.moveItemStackTo(stack, 6, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 從背包放入價格設定區
            if (!this.moveItemStackTo(stack, 0, 6, false)) {
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.hasPermissions(2);
    }
}
