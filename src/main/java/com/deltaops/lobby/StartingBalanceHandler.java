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
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7使用 /dt menu 開啟小隊面板，/dt stash 開啟倉庫，/dt sellgui 賣出物品。"));
        }
    }
}
