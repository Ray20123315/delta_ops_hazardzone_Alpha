/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.team;

import com.deltaops.DeltaOpsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class SpectatorHandler {
    private static final Set<UUID> ACTIVE_BATTLE_PLAYERS = ConcurrentHashMap.newKeySet();

    public static void markBattlePlayer(ServerPlayer player) {
        if (player != null) {
            ACTIVE_BATTLE_PLAYERS.add(player.getUUID());
        }
    }

    public static void clearBattlePlayer(ServerPlayer player) {
        if (player != null) {
            ACTIVE_BATTLE_PLAYERS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        if (!ACTIVE_BATTLE_PLAYERS.contains(player.getUUID())) {
            return;
        }

        event.setCanceled(true);
        player.setGameMode(GameType.SPECTATOR);
        player.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal("§7" + player.getGameProfile().getName() + " 已陣亡，進入觀戰模式。"),
                false
        );

        // 只跟隨同小隊的存活隊友，避免看到敵方位置
        com.deltaops.lobby.LobbySquadManager.Squad squad =
                com.deltaops.lobby.LobbySquadManager.getSquadByPlayer(player.getUUID());
        if (squad != null) {
            for (UUID memberId : squad.members) {
                if (memberId.equals(player.getUUID())) continue;
                ServerPlayer teammate = player.getServer().getPlayerList().getPlayer(memberId);
                if (teammate != null && teammate.isAlive()) {
                    player.setCamera(teammate);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ACTIVE_BATTLE_PLAYERS.contains(player.getUUID())) {
            return;
        }

        clearBattlePlayer(player);
        player.getInventory().clearContent();
        player.getEnderChestInventory().clearContent();
        player.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal("§c" + player.getGameProfile().getName() + " 於對局中強退，戰鬥失敗。"),
                false
        );
    }

    /**
     * 每 tick 檢查：
     * 1. 強制旁觀者鏡頭鎖定在存活隊友身上，防止自由觀看敵方位置。
     * 2. 觀戰者切換 hotbar slot 時切換跟隨的隊友。
     * 3. 全隊陣亡時自動返回大廳。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!ACTIVE_BATTLE_PLAYERS.contains(player.getUUID())) return;
        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) return;

        com.deltaops.lobby.LobbySquadManager.Squad squad =
                com.deltaops.lobby.LobbySquadManager.getSquadByPlayer(player.getUUID());

        // 全隊陣亡檢查（包含自己）
        if (squad != null) {
            boolean anyAlive = false;
            for (UUID memberId : squad.members) {
                ServerPlayer mp = player.getServer().getPlayerList().getPlayer(memberId);
                if (mp != null && mp.isAlive()) {
                    anyAlive = true;
                    break;
                }
            }
            if (!anyAlive) {
                // 全隊陣亡 → 返回大廳
                player.setGameMode(GameType.SURVIVAL);
                com.deltaops.location.LocationDatabase.teleportToLobby(player);
                clearBattlePlayer(player);
                return;
            }
        }

        // 偵測玩家切換 hotbar slot 來切換跟隨隊友
        int currentSlot = player.getInventory().selected;
        if (currentSlot != lastSlot.getOrDefault(player.getUUID(), 0)) {
            lastSlot.put(player.getUUID(), currentSlot);
            if (squad != null) {
                // 收集存活隊友（排除自己）
                List<ServerPlayer> aliveTeammates = new ArrayList<>();
                for (UUID memberId : squad.members) {
                    if (memberId.equals(player.getUUID())) continue;
                    ServerPlayer mp = player.getServer().getPlayerList().getPlayer(memberId);
                    if (mp != null && mp.isAlive()) {
                        aliveTeammates.add(mp);
                    }
                }
                // Slot 0 = 退出觀戰返回大廳
                if (currentSlot == 0) {
                    player.setGameMode(GameType.SURVIVAL);
                    com.deltaops.location.LocationDatabase.teleportToLobby(player);
                    clearBattlePlayer(player);
                    return;
                }
                // Slot 1~8 對應隊友索引 (1→第0個, 2→第1個, ...)
                int targetIndex = currentSlot - 1;
                if (targetIndex >= 0 && targetIndex < aliveTeammates.size()) {
                    ServerPlayer target = aliveTeammates.get(targetIndex);
                    if (target != null && target.isAlive()) {
                        player.setCamera(target);
                    }
                }
            }
        }

        // 若當前沒有跟隨實體，或跟隨的不是存活隊友，就重新尋找
        Entity camera = player.getCamera();
        if (camera == null || camera == player || (camera instanceof ServerPlayer cp && !cp.isAlive())) {
            if (squad != null) {
                for (UUID memberId : squad.members) {
                    if (memberId.equals(player.getUUID())) continue;
                    ServerPlayer teammate = player.getServer().getPlayerList().getPlayer(memberId);
                    if (teammate != null && teammate.isAlive()) {
                        player.setCamera(teammate);
                        break;
                    }
                }
            }
        }
    }

    private static final java.util.Map<UUID, Integer> lastSlot = new java.util.concurrent.ConcurrentHashMap<>();
}
