/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.lobby;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchmakingEngine {
    private static final Queue<UUID> soloPool = new ConcurrentLinkedQueue<>(); // player UUIDs
    private static final Queue<UUID> squadPool = new ConcurrentLinkedQueue<>(); // squad UUIDs
    private static final List<Match> upcomingMatches = Collections.synchronizedList(new ArrayList<>());

    public static void startMatchByLeader(ServerPlayer leader) {
        if (leader == null) return;
        LobbySquadManager.Squad squad = LobbySquadManager.getSquadByPlayer(leader.getUUID());
        if (squad == null) return;

        if (!squad.fillTeammates) {
            // immediate match with only squad members
            List<ServerPlayer> players = gatherPlayersFromSquad(squad);
            launchMatch(players);
            return;
        }

        // auto-fill behavior: add squad to squadPool and attempt to fill
        squadPool.add(squad.squadId);
        attemptFillAndLaunch();
    }

    private static List<ServerPlayer> gatherPlayersFromSquad(LobbySquadManager.Squad squad) {
        List<ServerPlayer> result = new ArrayList<>();
        if (squad == null) return result;
        MinecraftServer server = java.util.Objects.requireNonNullElseGet(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), () -> null);
        if (server == null) return result;
        for (UUID member : squad.members) {
            ServerPlayer sp = server.getPlayerList().getPlayer(member);
            if (sp != null) result.add(sp);
        }
        return result;
    }

    private static void attemptFillAndLaunch() {
        // very basic fill: if there are at least two squads, merge them; or fill with solos
        while (!squadPool.isEmpty()) {
            UUID squadId = squadPool.poll();
            LobbySquadManager.Squad s = LobbySquadManager.getSquad(squadId);
            if (s == null) continue;
            int needed = s.limit - s.members.size();
            List<ServerPlayer> players = gatherPlayersFromSquad(s);
            // try to fill from soloPool
            while (needed > 0 && !soloPool.isEmpty()) {
                UUID solo = soloPool.poll();
                ServerPlayer sp = java.util.Objects.requireNonNullElseGet(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), () -> null).getPlayerList().getPlayer(solo);
                if (sp != null) {
                    players.add(sp);
                    needed--;
                }
            }
            // if still need, try to merge other squads
            while (needed > 0 && !squadPool.isEmpty()) {
                UUID otherId = squadPool.poll();
                LobbySquadManager.Squad other = LobbySquadManager.getSquad(otherId);
                if (other == null) continue;
                List<ServerPlayer> otherPlayers = gatherPlayersFromSquad(other);
                for (ServerPlayer p : otherPlayers) {
                    if (needed <= 0) break;
                    players.add(p);
                    needed--;
                }
            }

            // launch even if not full
            launchMatch(players);
        }
    }

    private static void launchMatch(List<ServerPlayer> players) {
        launchMatch(players, null, null);
    }

    /**
     * Launch match and teleport players. If mapName is provided and a spawn is configured, teleport to that spawn.
     */
    public static void launchMatch(List<ServerPlayer> players, String mapName) {
        launchMatch(players, mapName, null);
    }

    public static void launchMatch(List<ServerPlayer> players, String mapName, ServerPlayer leader) {
        if (players == null || players.isEmpty()) return;
        if (leader != null && mapName != null && !mapName.isBlank()) {
            MapDefinition mapDefinition = HazardMapRegistry.getMap(mapName);
            if (mapDefinition != null) {
                int threshold = mapDefinition.minGearValue();
                for (ServerPlayer member : players) {
                    int gearValue = GearValueEvaluator.calculatePlayerGearValue(member);
                    if (gearValue < threshold) {
                        if (leader != null) {
                            leader.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    String.format("§c[Delta Ops] ❌ 無法部署！隊員 [%s] 的戰備值 (%d) 未達地圖 [%s] 的最低要求 (%d 哈夫幣)。",
                                            member.getGameProfile().getName(), gearValue, mapDefinition.displayName(), threshold)
                            ));
                        }
                        member.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§c[Delta Ops] ⚠️ 你的戰備值過低，無法進入此高難度區域！"
                        ));
                        return;
                    }
                }
            }
        }

        MinecraftServer server = java.util.Objects.requireNonNullElseGet(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), () -> null);
        if (server == null) return;
        ServerLevel target = server.overworld();

        // ========== 重置容器：清除舊箱子 & 生成新箱子 ==========
        resetContainersForMatch(target, mapName);

        // try to use configured spawn point
        if (mapName != null) {
            Optional<com.deltaops.location.LocationDatabase.SpawnPoint> opt = com.deltaops.location.LocationDatabase.getRandomSpawnPoint(mapName);
            if (opt.isPresent()) {
                com.deltaops.location.LocationDatabase.SpawnPoint sp = opt.get();
                int idx = 0;
                for (ServerPlayer p : players) {
                    try {
                        p.teleportTo(target, sp.x + idx * 0.5, sp.y, sp.z + idx * 0.5, sp.yaw, sp.pitch);
                    } catch (Exception ignored) {}
                    idx++;
                }
                // 標記為對戰中玩家（死亡時進入觀戰模式）
                for (ServerPlayer p : players) {
                    com.deltaops.team.SpectatorHandler.markBattlePlayer(p);
                }
                return;
            }
        }

        // fallback: world spawn
        teleportPlayersAndMarkBattle(players, target, mapName);
    }

    private static void resetContainersForMatch(ServerLevel level, String mapName) {
        // 1. 只載入該地圖對應的 Zone（mapName=null 時載入全部）
        java.util.Map<String, com.deltaops.zone.ZoneSelectionManager.Zone> zones;
        if (mapName != null && !mapName.isBlank()) {
            zones = com.deltaops.zone.ZoneSelectionManager.loadZonesForMap(mapName);
            // 若該地圖沒有專屬 Zone，補載入無綁定 (mapId="") 的通用 Zone
            if (zones.isEmpty()) {
                java.util.Map<String, com.deltaops.zone.ZoneSelectionManager.Zone> all =
                        com.deltaops.zone.ZoneSelectionManager.loadZonesMap();
                for (java.util.Map.Entry<String, com.deltaops.zone.ZoneSelectionManager.Zone> entry : all.entrySet()) {
                    if (entry.getValue().mapId == null || entry.getValue().mapId.isEmpty()) {
                        zones.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } else {
            zones = com.deltaops.zone.ZoneSelectionManager.loadZonesMap();
        }
        if (zones.isEmpty()) return;

        double minX = Double.MAX_VALUE, minY = 0, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = level.getMaxBuildHeight(), maxZ = -Double.MAX_VALUE;
        for (com.deltaops.zone.ZoneSelectionManager.Zone z : zones.values()) {
            net.minecraft.core.BlockPos lo = z.min();
            net.minecraft.core.BlockPos hi = z.max();
            if (lo.getX() < minX) minX = lo.getX();
            if (lo.getZ() < minZ) minZ = lo.getZ();
            if (hi.getX() > maxX) maxX = hi.getX();
            if (hi.getZ() > maxZ) maxZ = hi.getZ();
        }
        AABB worldZone = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        // 2. 清除範圍內所有舊容器
        com.deltaops.container.LootContainerSpawner.clearAllContainersInZone(level, worldZone);

        // 3. 生成固定高價值容器 (Fixed Anchors)
        if (mapName != null && !mapName.isBlank()) {
            com.deltaops.container.FixedLootManager.spawnFixedAnchors(level, mapName);
        }

        // 4. 隨機分佈戰利品容器
        com.deltaops.container.GridDistributionEngine.distributeRandomLoot(level, worldZone, 0.0006, 24);
    }

    private static void teleportPlayersAndMarkBattle(List<ServerPlayer> players, ServerLevel target, String mapName) {
        BlockPos spawn = target.getSharedSpawnPos();
        int idx = 0;
        for (ServerPlayer p : players) {
            try {
                p.teleportTo(target, spawn.getX() + idx * 2, spawn.getY(), spawn.getZ() + idx * 2, p.getYRot(), p.getXRot());
            } catch (Exception ignored) {}
            idx++;
        }
        // 標記為對戰中玩家（死亡時進入觀戰模式）
        for (ServerPlayer p : players) {
            com.deltaops.team.SpectatorHandler.markBattlePlayer(p);
        }
    }

    private static class Match {
        public final UUID matchId = UUID.randomUUID();
        public final List<UUID> playerUuids = new ArrayList<>();
    }

    public static void enqueueSoloPlayer(ServerPlayer player) {
        if (player == null) return;
        soloPool.add(player.getUUID());
    }

    /**
     * 將小隊加入配對佇列。
     */
    public static void enqueueSquad(UUID squadId) {
        if (squadId == null) return;
        if (!squadPool.contains(squadId)) {
            squadPool.add(squadId);
        }
    }

    /**
     * 回傳佇列中的玩家人數 (單人 + 小隊成員估算)。
     */
    public static int getQueuedPlayerCount() {
        return soloPool.size() + squadPool.size() * 4;
    }

    /**
     * 是否有任何佇列正在進行配對。
     */
    public static boolean isAnyQueued() {
        return !soloPool.isEmpty() || !squadPool.isEmpty();
    }
}
