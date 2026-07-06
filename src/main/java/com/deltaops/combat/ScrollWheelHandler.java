/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.combat;

import com.deltaops.DeltaOpsMod;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 滾輪事件攔截：滾輪切換快捷欄時，
 * 目標欄位的物品若不是武器/刀/投擲物，則跳過。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, value = Dist.CLIENT)
public class ScrollWheelHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;

        Inventory inv = mc.player.getInventory();
        double delta = event.getScrollDelta();

        // 計算滾輪後的目標欄位
        int currentSlot = inv.selected;
        int targetSlot;
        if (delta > 0) {
            targetSlot = (currentSlot - 1 + 9) % 9;
        } else {
            targetSlot = (currentSlot + 1) % 9;
        }

        // 檢查目標欄位是否允許切換
        ItemStack targetStack = inv.getItem(targetSlot);
        if (!targetStack.isEmpty() && !WeaponDetection.isCombatItem(targetStack)) {
            // 不准切到非戰鬥物品，取消事件
            event.setCanceled(true);
        }
    }
}
