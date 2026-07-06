/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.network.sync;

import com.deltaops.DeltaOpsMod;
import com.deltaops.lobby.EconomyManager;
import com.deltaops.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 設定檔同步廣播器。
 * 當設定檔重新載入後，向所有線上玩家發送 SyncConfigPacket。
 */
public class SyncConfigBroadcaster {

    /**
     * 向所有線上玩家廣播設定檔同步通知。
     */
    public static void broadcast() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        SyncConfigPacket packet = new SyncConfigPacket(
                EconomyManager.getItemCount(),
                System.currentTimeMillis()
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    packet
            );
        }
        DeltaOpsMod.LOGGER.info("📡 [Delta Ops] 設定檔同步已廣播給 {} 位玩家", server.getPlayerList().getPlayerCount());
    }
}
