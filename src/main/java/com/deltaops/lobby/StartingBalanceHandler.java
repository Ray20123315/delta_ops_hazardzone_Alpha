/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.lobby;

import com.deltaops.DeltaOpsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 新玩家初始資金：首次加入伺服器時給予 2,000,000 哈夫幣。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class StartingBalanceHandler {

    private static final long STARTING_BALANCE = 2_000_000L;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        long current = EconomyManager.getBalance(player);
        if (current <= 0L) {
            EconomyManager.addBalance(player, STARTING_BALANCE);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6=== Delta Ops: Hazard Zone ==="));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§e歡迎，特遣隊員！你的初始資金 §6" + STARTING_BALANCE + " §e哈夫幣已入帳。"));
        }

        // ===== 所有玩家登入時提示指令用法 =====
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§m========================================"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§l  Delta Ops: Hazard Zone  §7v" + DeltaOpsMod.MOD_VERSION));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§m========================================"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§e📋 常用指令:"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                " §a/dt menu §7- 開啟小隊面板（建立/邀請/準備/開始遊戲）"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                " §a/dt stash §7- 開啟個人倉庫（9×6 儲存空間）"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                " §a/dt shop §7- 開啟商人商店購買物品"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                " §a/dt sellgui §7- 賣出倉庫中的物品"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                " §a/dt quests §7- 查看每日任務"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                " §a/dt skills §7- 查看/升級技能"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                " §a/dt balance §7- 查詢哈夫幣餘額"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                " §a/dt trade <玩家> §7- 與其他玩家交易物品"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§m========================================"));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
    }
}
