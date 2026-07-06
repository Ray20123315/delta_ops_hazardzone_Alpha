/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.weapon;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class WeaponWorkbenchMenu extends AbstractContainerMenu {
    private final ItemStackHandler weaponSlot = new ItemStackHandler(1);
    private final ItemStackHandler attachmentSlot = new ItemStackHandler(1);

    public WeaponWorkbenchMenu(int id, Inventory inventory) {
        super(null, id);

        // 武器槽
        this.addSlot(new SlotItemHandler(weaponSlot, 0, 26, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty();
            }
        });

        // 配件槽
        this.addSlot(new SlotItemHandler(attachmentSlot, 0, 62, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty();
            }
        });

        // 玩家背包
        int startY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, row * 9 + col + 9, 8 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, startY + 58));
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
     * 安裝配件：將配件槽中的配件安裝到武器上。
     */
    public ItemStack attachAttachment() {
        ItemStack weapon = weaponSlot.getStackInSlot(0).copy();
        ItemStack attachment = attachmentSlot.getStackInSlot(0);
        if (weapon.isEmpty() || attachment.isEmpty()) return ItemStack.EMPTY;

        // 從附件物品的 NBT 讀取配件類型與 ID
        if (attachment.getTag() == null) return ItemStack.EMPTY;
        String attTypeStr = attachment.getTag().getString("attachment_type");
        String attId = attachment.getTag().getString("attachment_id");
        if (attTypeStr.isEmpty() || attId.isEmpty()) return ItemStack.EMPTY;

        WeaponAttachment.AttachmentType type = WeaponAttachment.AttachmentType.fromTagName(attTypeStr);
        if (type == null) return ItemStack.EMPTY;

        ItemStack result = WeaponAttachment.attach(weapon, type, attId);
        // 消耗配件
        attachmentSlot.getStackInSlot(0).shrink(1);
        return result;
    }

    /**
     * 卸載配件：從武器卸下指定類型的配件。
     */
    public ItemStack removeAttachment(WeaponAttachment.AttachmentType type) {
        ItemStack weapon = weaponSlot.getStackInSlot(0).copy();
        if (weapon.isEmpty()) return ItemStack.EMPTY;

        String current = WeaponAttachment.getAttachment(weapon, type);
        if (current.isEmpty()) return ItemStack.EMPTY;

        WeaponAttachment.removeAttachment(weapon, type);
        return weapon;
    }

    public ItemStack getWeapon() {
        return weaponSlot.getStackInSlot(0);
    }
}
