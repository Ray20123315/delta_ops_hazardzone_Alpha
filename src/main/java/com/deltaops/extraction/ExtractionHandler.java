/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.extraction;

import com.deltaops.DeltaOpsMod;
import com.deltaops.block.ModBlocks;
import com.deltaops.config.ModConfig;
import com.deltaops.loading.LoadingScreenManager;
import com.deltaops.lobby.EconomyManager;
import com.deltaops.team.LobbyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class ExtractionHandler {
    private static final Map<UUID, Integer> extracting = new HashMap<>();

    private static int getExtractionTicks() {
        return ModConfig.getExtractionTimerSeconds() * 20;
    }

    public static boolean isInExtractionZone(Player player) {
        if (player == null || player.level().isClientSide) {
            return false;
        }
        return ExtractionPointManager.getExtractionMapForPlayer(player).isPresent();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        Player player = event.player;
        UUID uuid = player.getUUID();

        if (isInExtractionZone(player)) {
            int ticks = extracting.getOrDefault(uuid, getExtractionTicks()) - 1;
            extracting.put(uuid, ticks);

            if (ticks % 20 == 0 || ticks <= 20) {
                int sec = ticks / 20 + 1;
                player.displayClientMessage(Component.literal("§e撤離中：" + sec + " 秒"), true);
            }

            if (ticks <= 0) {
                extracting.remove(uuid);
                if (player instanceof ServerPlayer sp) {
                    completeExtraction(sp);
                }
            }
        } else if (extracting.containsKey(uuid)) {
            extracting.remove(uuid);
            player.displayClientMessage(Component.literal("§c撤離倒數已取消。"), true);
        }
    }

    public static void completeExtraction(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        String mapName = ExtractionPointManager.getExtractionMapForPlayer(player).orElse(null);

        // 僅給予地圖完成獎勵（不自動賣出物品），物品保留在背包供倉庫/交易使用
        long bonus = 0L;
        if (mapName != null && !mapName.isBlank()) {
            com.deltaops.lobby.MapDefinition mapDef = com.deltaops.lobby.HazardMapRegistry.getMap(mapName);
            if (mapDef != null) {
                bonus = Math.max(0L, mapDef.minGearValue() / 10L);
            }
        }
        double mult = com.deltaops.config.ModConfig.getRewardMultiplier();
        long reward = Math.max(0L, (long) (bonus * mult));
        if (reward > 0L) {
            com.deltaops.lobby.EconomyManager.addBalance(player, reward);
        }

        MinecraftServer server = player.server;
        if (server == null) {
            return;
        }

        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());

        LobbyManager.teleportToPersonalLobby(player);
        player.displayClientMessage(Component.literal("§a撤離成功，已返回大廳。獲得報酬：" + reward + " 哈夫幣。"), false);
        player.sendSystemMessage(Component.literal("§e你的物品已保留在背包中，可使用 §6/dt stash §e開啟倉庫儲存，或使用 §6/dt sellgui §e賣出。"));
    }
}
