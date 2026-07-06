/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.combat;

import com.deltaops.DeltaOpsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 快捷欄限制：定期檢查所有玩家的快捷欄，
 * 把非戰鬥物品（非武器/刀/投擲物）自動移到背包，
 * 並自動把背包中的戰鬥物品補到快捷欄的空位。
 *
 * 遊戲開始前（大廳中）也生效。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class HotbarRestrictionHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        Inventory inv = player.getInventory();
        boolean changed = false;

        // 1. 把快捷欄中的非戰鬥物品移到背包
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack stack = inv.getItem(hotbarSlot);
            if (stack.isEmpty()) continue;
            if (WeaponDetection.isCombatItem(stack)) continue;

            // 非戰鬥物品，移到背包（主物品欄第 9~35 格）
            for (int invSlot = 9; invSlot < 36; invSlot++) {
                ItemStack target = inv.getItem(invSlot);
                if (target.isEmpty()) {
                    inv.setItem(invSlot, stack.copy());
                    inv.setItem(hotbarSlot, ItemStack.EMPTY);
                    changed = true;
                    break;
                }
            }
        }

        // 2. 把背包中的戰鬥物品補到快捷欄空位
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            if (!inv.getItem(hotbarSlot).isEmpty()) continue;

            // 從背包找戰鬥物品補過來
            for (int invSlot = 9; invSlot < 36; invSlot++) {
                ItemStack stack = inv.getItem(invSlot);
                if (stack.isEmpty()) continue;
                if (!WeaponDetection.isCombatItem(stack)) continue;

                inv.setItem(hotbarSlot, stack.copy());
                inv.setItem(invSlot, ItemStack.EMPTY);
                changed = true;
                break;
            }
        }

        if (changed) {
            inv.setChanged();
        }
    }
}
