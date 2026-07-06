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
 * 武器數量限制：
 * - 最多 3 把槍械（TaCZ + 弩）
 * - 超出時自動將多餘的武器移到背包末端
 *
 * 只在遊戲開始前（大廳中）生效，進入對戰後不限制。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class WeaponLimitHandler {

    private static final int MAX_GUNS = 3;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // 檢查是否在大廳中（距離重生點 2000 格內）
        if (!com.deltaops.lobby.LobbyArea.isInLobby(player)) return;

        Inventory inv = player.getInventory();
        boolean changed = false;

        // 掃描整個背包 + 快捷欄計算槍械數量
        int gunCount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (WeaponDetection.isGun(stack)) gunCount++;
        }

        // 超出上限時，從快捷欄優先移除多餘的武器到背包末端
        if (gunCount > MAX_GUNS) {
            int excess = gunCount - MAX_GUNS;

            for (int i = 0; i < 36 && excess > 0; i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) continue;
                if (!WeaponDetection.isGun(stack)) continue;

                // 移到背包末端（找空位）
                for (int j = 36; j < inv.getContainerSize(); j++) {
                    if (inv.getItem(j).isEmpty()) {
                        inv.setItem(j, stack.copy());
                        inv.setItem(i, ItemStack.EMPTY);
                        changed = true;
                        excess--;
                        break;
                    }
                }
            }
        }

        if (changed) {
            inv.setChanged();
        }
    }
}
