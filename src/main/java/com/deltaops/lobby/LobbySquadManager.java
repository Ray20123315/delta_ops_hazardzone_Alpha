/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.lobby;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LobbySquadManager {
    private static final Map<UUID, Squad> SQUADS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_SQUAD = new ConcurrentHashMap<>();
    private static final int DEFAULT_LIMIT = 3;

    public static Squad createSquad(ServerPlayer player) {
        if (player == null) return null;
        Squad existing = getSquadByPlayer(player.getUUID());
        if (existing != null) {
            player.sendSystemMessage(Component.literal("你已經在一個小隊中。"));
            return existing;
        }
        UUID squadId = UUID.randomUUID();
        Squad s = new Squad(squadId, player.getUUID());
        s.members.add(player.getUUID());
        SQUADS.put(squadId, s);
        PLAYER_TO_SQUAD.put(player.getUUID(), squadId);
        READY.put(player.getUUID(), false);
        player.sendSystemMessage(Component.literal("已建立小隊。"));
        broadcastSquadStatus(s);
        return s;
    }

    public static Squad createOrGetSquad(ServerPlayer player) {
        if (player == null) return null;
        Squad existing = getSquadByPlayer(player.getUUID());
        if (existing != null) return existing;
        return createSquad(player);
    }

    public static boolean joinSquad(ServerPlayer player, UUID squadId) {
        if (player == null || squadId == null) return false;
        Squad s = SQUADS.get(squadId);
        if (s == null) return false;
        if (s.members.contains(player.getUUID())) return false;
        if (s.members.size() >= s.limit) return false;
        s.members.add(player.getUUID());
        PLAYER_TO_SQUAD.put(player.getUUID(), squadId);
        READY.put(player.getUUID(), false);
        ServerPlayer leader = getServerPlayerByUuid(s.leaderUuid);
        if (leader != null) leader.sendSystemMessage(Component.literal(player.getGameProfile().getName() + " 已加入小隊。"));
        player.sendSystemMessage(Component.literal("你已加入小隊。"));
        broadcastSquadStatus(s);
        return true;
    }

    public static boolean leaveSquad(ServerPlayer player) {
        if (player == null) return false;
        UUID squadId = PLAYER_TO_SQUAD.get(player.getUUID());
        if (squadId == null) return false;
        Squad s = SQUADS.get(squadId);
        if (s == null) return false;
        s.members.remove(player.getUUID());
        PLAYER_TO_SQUAD.remove(player.getUUID());
        READY.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("你已離開小隊。"));
        if (s.leaderUuid.equals(player.getUUID())) {
            // disband or transfer leadership
            if (!s.members.isEmpty()) {
                UUID newLeader = s.members.get(0);
                s.leaderUuid = newLeader;
                ServerPlayer newLeaderPlayer = getServerPlayerByUuid(newLeader);
                if (newLeaderPlayer != null) newLeaderPlayer.sendSystemMessage(Component.literal("你已成為新的隊長。"));
                broadcastSquadStatus(s);
            } else {
                SQUADS.remove(squadId);
            }
        } else {
            broadcastSquadStatus(s);
        }
        return true;
    }

    public static boolean kickFromSquad(ServerPlayer leader, ServerPlayer member) {
        if (leader == null || member == null) return false;
        UUID squadId = PLAYER_TO_SQUAD.get(leader.getUUID());
        if (squadId == null) return false;
        Squad s = SQUADS.get(squadId);
        if (s == null || !s.leaderUuid.equals(leader.getUUID())) return false;
        if (!s.members.contains(member.getUUID()) || member.getUUID().equals(leader.getUUID())) return false;
        s.members.remove(member.getUUID());
        PLAYER_TO_SQUAD.remove(member.getUUID());
        READY.remove(member.getUUID());
        leader.sendSystemMessage(Component.literal(member.getGameProfile().getName() + " 已從小隊中被移除。"));
        member.sendSystemMessage(Component.literal("你已被移出小隊。"));
        broadcastSquadStatus(s);
        return true;
    }

    public static boolean transferLeadership(ServerPlayer leader, ServerPlayer newLeader) {
        if (leader == null || newLeader == null) return false;
        UUID squadId = PLAYER_TO_SQUAD.get(leader.getUUID());
        if (squadId == null) return false;
        Squad s = SQUADS.get(squadId);
        if (s == null || !s.leaderUuid.equals(leader.getUUID())) return false;
        if (!s.members.contains(newLeader.getUUID()) || newLeader.getUUID().equals(leader.getUUID())) return false;
        s.leaderUuid = newLeader.getUUID();
        leader.sendSystemMessage(Component.literal("你已將隊長權限移交給 " + newLeader.getGameProfile().getName() + "。"));
        newLeader.sendSystemMessage(Component.literal("你已成為新的隊長。"));
        broadcastSquadStatus(s);
        return true;
    }

    public static void toggleAutoMatch(ServerPlayer player) {
        if (player == null) return;
        UUID squadId = PLAYER_TO_SQUAD.get(player.getUUID());
        if (squadId == null) return;
        Squad s = SQUADS.get(squadId);
        if (s == null) return;
        s.fillTeammates = !s.fillTeammates;
        player.sendSystemMessage(Component.literal("自動配對: " + (s.fillTeammates ? "已啟用" : "已停用")));

        // 更新配對佇列並通知客戶端
        if (s.fillTeammates) {
            MatchmakingEngine.enqueueSquad(squadId);
        }
        // 發送佇列狀態給小隊所有成員
        broadcastMatchStatus(s);
    }

    /**
     * 發送 ClientboundMatchStatusPacket 給小隊所有成員。
     */
    public static void broadcastMatchStatus(Squad squad) {
        if (squad == null) return;
        int queued = MatchmakingEngine.getQueuedPlayerCount();
        boolean matchOpen = MatchmakingEngine.isAnyQueued();
        var packet = new com.deltaops.network.ClientboundMatchStatusPacket(queued, matchOpen);
        for (UUID member : squad.members) {
            ServerPlayer p = getServerPlayerByUuid(member);
            if (p != null) {
                com.deltaops.network.ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p), packet);
            }
        }
    }

    public static boolean toggleReady(ServerPlayer player) {
        if (player == null) return false;
        UUID uuid = player.getUUID();
        boolean ready = !isReady(uuid);
        READY.put(uuid, ready);
        Squad squad = getSquadByPlayer(uuid);
        if (squad != null) {
            broadcastSquadStatus(squad);
        }
        player.sendSystemMessage(Component.literal("準備狀態：" + (ready ? "已準備" : "未準備")));
        return true;
    }

    public static boolean attemptLaunch(ServerPlayer leader) {
        if (leader == null) return false;
        Squad squad = getSquadByPlayer(leader.getUUID());
        if (squad == null || !squad.leaderUuid.equals(leader.getUUID())) return false;
        if (!isSquadReady(squad)) {
            for (UUID member : squad.members) {
                if (!isReady(member)) {
                    ServerPlayer p = getServerPlayerByUuid(member);
                    if (p != null) {
                        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("你尚未準備，無法發車。"));
                    }
                }
            }
            leader.sendSystemMessage(net.minecraft.network.chat.Component.literal("發車失敗：並非所有隊員皆已準備。"));
            return false;
        }

        // 檢查地圖與人數限制
        String mapId = squad.mapId;
        if (mapId == null || mapId.isBlank()) {
            leader.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c請先選擇地圖再發車！"));
            return false;
        }
        MapDefinition mapDef = HazardMapRegistry.getMap(mapId);
        if (mapDef == null) {
            leader.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c地圖不存在：" + mapId));
            return false;
        }
        if (squad.members.size() < mapDef.minPlayers()) {
            leader.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c玩家人數不足！地圖 [" + mapDef.displayName() + "] 最少需 " + mapDef.minPlayers() + " 人。"));
            return false;
        }
        if (squad.members.size() > mapDef.maxPlayers()) {
            leader.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c玩家人數過多！地圖 [" + mapDef.displayName() + "] 最多 " + mapDef.maxPlayers() + " 人。"));
            return false;
        }

        List<ServerPlayer> players = gatherPlayersFromSquad(squad);
        MatchmakingEngine.launchMatch(players, mapId, leader);
        return true;
    }

    private static List<ServerPlayer> gatherPlayersFromSquad(Squad squad) {
        List<ServerPlayer> result = new ArrayList<>();
        if (squad == null) return result;
        var server = java.util.Objects.requireNonNullElseGet(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), () -> null);
        if (server == null) return result;
        for (UUID member : squad.members) {
            ServerPlayer sp = server.getPlayerList().getPlayer(member);
            if (sp != null) result.add(sp);
        }
        return result;
    }

    public static boolean isReady(UUID playerUuid) {
        return playerUuid != null && READY.getOrDefault(playerUuid, false);
    }

    public static boolean isSquadReady(Squad squad) {
        if (squad == null) return false;
        for (UUID member : squad.members) {
            if (!isReady(member)) {
                return false;
            }
        }
        return true;
    }

    public static Squad getSquad(UUID squadId) {
        return SQUADS.get(squadId);
    }

    public static Squad getSquadByPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        UUID squadId = PLAYER_TO_SQUAD.get(playerUuid);
        return squadId == null ? null : SQUADS.get(squadId);
    }

    public static void broadcastSquadStatus(Squad squad) {
        if (squad == null) return;
        List<UUID> uuids = new ArrayList<>(squad.members);
        List<String> names = new ArrayList<>();
        List<Boolean> readyStates = new ArrayList<>();
        for (UUID member : uuids) {
            ServerPlayer p = getServerPlayerByUuid(member);
            names.add(p != null ? p.getName().getString() : "Unknown");
            readyStates.add(isReady(member));
        }
        com.deltaops.network.squad.ClientboundSquadStatusPacket packet = new com.deltaops.network.squad.ClientboundSquadStatusPacket(squad.leaderUuid, uuids, names, readyStates);
        for (UUID member : uuids) {
            ServerPlayer p = getServerPlayerByUuid(member);
            if (p != null) {
                com.deltaops.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p), packet);
            }
        }
    }

    public static class Squad {
        public final UUID squadId;
        public UUID leaderUuid;
        public final List<UUID> members = new ArrayList<>();
        public boolean fillTeammates = true;
        public final int limit;
        public String mapId;

        public Squad(UUID id, UUID leader) {
            this.squadId = id;
            this.leaderUuid = leader;
            this.limit = DEFAULT_LIMIT;
        }
    }

    public static boolean setSquadMapId(ServerPlayer leader, String mapId) {
        if (leader == null || mapId == null || mapId.isBlank()) {
            return false;
        }
        Squad squad = getSquadByPlayer(leader.getUUID());
        if (squad == null || !squad.leaderUuid.equals(leader.getUUID())) {
            return false;
        }
        squad.mapId = mapId;
        leader.sendSystemMessage(Component.literal("已設定小隊地圖為：" + mapId));
        broadcastSquadStatus(squad);
        return true;
    }

    public static boolean attemptLaunch(ServerPlayer leader, String mapName) {
        if (leader == null || mapName == null || mapName.isBlank()) {
            return false;
        }
        boolean result = setSquadMapId(leader, mapName);
        if (!result) {
            return false;
        }
        return attemptLaunch(leader);
    }

    private static ServerPlayer getServerPlayerByUuid(UUID uuid) {
        if (uuid == null) return null;
        for (ServerPlayer sp : java.util.Objects.requireNonNullElseGet(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), () -> null).getPlayerList().getPlayers()) {
            if (sp.getUUID().equals(uuid)) return sp;
        }
        return null;
    }

    // Pending requests for invites/applications
    public static class PendingRequest {
        public final UUID sender;
        public final UUID target;
        public final RequestType type;
        public final long timestamp;

        public PendingRequest(UUID sender, UUID target, RequestType type) {
            this.sender = sender;
            this.target = target;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public enum RequestType { INVITE, APPLY }

    private static final ConcurrentMap<UUID, Boolean> READY = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, PendingRequest> PENDING = new ConcurrentHashMap<>();

    public static boolean setReady(ServerPlayer player, boolean ready) {
        if (player == null) return false;
        UUID uuid = player.getUUID();
        UUID squadId = PLAYER_TO_SQUAD.get(uuid);
        if (squadId == null) return false;
        READY.put(uuid, ready);
        Squad squad = SQUADS.get(squadId);
        if (squad != null) {
            broadcastSquadStatus(squad);
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("準備狀態：" + (ready ? "已準備" : "未準備")));
        return true;
    }

    public static void handlePlayerTargeting(ServerPlayer sender, ServerPlayer target) {
        if (sender == null || target == null) return;
        // if target has no squad -> invite
        Squad tgt = getSquadByPlayer(target.getUUID());
        if (tgt == null) {
            PendingRequest r = new PendingRequest(sender.getUUID(), target.getUUID(), RequestType.INVITE);
            PENDING.put(target.getUUID(), r);
            // send clientbound packet to target to show request screen
            com.deltaops.network.squad.ClientboundShowRequestScreenPacket pkt = new com.deltaops.network.squad.ClientboundShowRequestScreenPacket(sender.getUUID(), sender.getName().getString(), RequestType.INVITE.name());
            com.deltaops.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target), pkt);
            return;
        }

        // target in a squad -> this is an application to that squad
        UUID leader = tgt.leaderUuid;
        PendingRequest req = new PendingRequest(sender.getUUID(), leader, RequestType.APPLY);
        PENDING.put(leader, req);
        ServerPlayer leaderPlayer = getServerPlayerByUuid(leader);
        if (leaderPlayer != null) {
            com.deltaops.network.squad.ClientboundShowRequestScreenPacket pkt = new com.deltaops.network.squad.ClientboundShowRequestScreenPacket(sender.getUUID(), sender.getName().getString(), RequestType.APPLY.name());
            com.deltaops.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> leaderPlayer), pkt);
        }
    }

    public static PendingRequest consumePending(UUID recipient) {
        return PENDING.remove(recipient);
    }

    /**
     * 檢查兩名玩家是否在同一小隊。
     */
    public static boolean areInSameSquad(ServerPlayer p1, ServerPlayer p2) {
        if (p1 == null || p2 == null) return false;
        UUID squadId1 = PLAYER_TO_SQUAD.get(p1.getUUID());
        UUID squadId2 = PLAYER_TO_SQUAD.get(p2.getUUID());
        return squadId1 != null && squadId1.equals(squadId2);
    }

    /**
     * 取得小隊所有成員（ServerPlayer 列表）。
     */
    public static List<ServerPlayer> getSquadMembers(ServerPlayer player) {
        List<ServerPlayer> result = new ArrayList<>();
        if (player == null) return result;
        UUID squadId = PLAYER_TO_SQUAD.get(player.getUUID());
        if (squadId == null) return result;
        Squad squad = SQUADS.get(squadId);
        if (squad == null) return result;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return result;
        for (UUID member : squad.members) {
            ServerPlayer sp = server.getPlayerList().getPlayer(member);
            if (sp != null) result.add(sp);
        }
        return result;
    }
}
