/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.network.squad;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public class ServerboundSquadActionPacket {
    public enum Action { CREATE, LEAVE, INVITE, KICK, TRANSFER, TOGGLE_AUTO_MATCH, TOGGLE_READY, LAUNCH, SELECT_MAP, OPEN_SHOP }

    public final Action action;
    public final String targetName; // optional for invite/kick/transfer

    public ServerboundSquadActionPacket(Action action, String targetName) {
        this.action = Objects.requireNonNull(action);
        this.targetName = targetName;
    }

    public static void encode(ServerboundSquadActionPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.action.name());
        buf.writeBoolean(pkt.targetName != null);
        if (pkt.targetName != null) buf.writeUtf(pkt.targetName);
    }

    public static ServerboundSquadActionPacket decode(FriendlyByteBuf buf) {
        Action a = Action.valueOf(buf.readUtf());
        String t = null;
        if (buf.readBoolean()) t = buf.readUtf();
        return new ServerboundSquadActionPacket(a, t);
    }

    public static void handle(ServerboundSquadActionPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            switch (pkt.action) {
                case CREATE -> com.deltaops.lobby.LobbySquadManager.createOrGetSquad(sender);
                case LEAVE -> com.deltaops.lobby.LobbySquadManager.leaveSquad(sender);
                case TOGGLE_AUTO_MATCH -> com.deltaops.lobby.LobbySquadManager.toggleAutoMatch(sender);
                case INVITE -> {
                    if (pkt.targetName == null || pkt.targetName.isBlank()) {
                        sender.sendSystemMessage(Component.literal("請輸入要邀請的玩家名稱。"));
                        break;
                    }
                    var server = sender.getServer();
                    if (server == null) break;
                    var tgt = server.getPlayerList().getPlayerByName(pkt.targetName);
                    if (tgt == null) {
                        sender.sendSystemMessage(Component.literal("找不到玩家：" + pkt.targetName));
                        break;
                    }
                    com.deltaops.lobby.LobbySquadManager.handlePlayerTargeting(sender, tgt);
                }
                case KICK -> {
                    if (pkt.targetName == null || pkt.targetName.isBlank()) {
                        sender.sendSystemMessage(Component.literal("請輸入要踢出的玩家名稱。"));
                        break;
                    }
                    var server = sender.getServer();
                    if (server == null) break;
                    var tgt = server.getPlayerList().getPlayerByName(pkt.targetName);
                    if (tgt == null) {
                        sender.sendSystemMessage(Component.literal("找不到玩家：" + pkt.targetName));
                        break;
                    }
                    if (!com.deltaops.lobby.LobbySquadManager.kickFromSquad(sender, tgt)) {
                        sender.sendSystemMessage(Component.literal("踢出失敗，請確認你是隊長且玩家在你的小隊中。"));
                    }
                }
                case TRANSFER -> {
                    if (pkt.targetName == null || pkt.targetName.isBlank()) {
                        sender.sendSystemMessage(Component.literal("請輸入要移交的玩家名稱。"));
                        break;
                    }
                    var server = sender.getServer();
                    if (server == null) break;
                    var tgt = server.getPlayerList().getPlayerByName(pkt.targetName);
                    if (tgt == null) {
                        sender.sendSystemMessage(Component.literal("找不到玩家：" + pkt.targetName));
                        break;
                    }
                    if (!com.deltaops.lobby.LobbySquadManager.transferLeadership(sender, tgt)) {
                        sender.sendSystemMessage(Component.literal("移交失敗，請確認你是隊長且玩家在你的小隊中。"));
                    }
                }
                case TOGGLE_READY -> com.deltaops.lobby.LobbySquadManager.toggleReady(sender);
                case LAUNCH -> com.deltaops.lobby.LobbySquadManager.attemptLaunch(sender);
                case SELECT_MAP -> {
                    String mapId = pkt.targetName; // 服用 targetName 欄位存放地圖 ID
                    if (mapId != null && !mapId.isBlank()) {
                        com.deltaops.lobby.LobbySquadManager.setSquadMapId(sender, mapId);
                    }
                }
                case OPEN_SHOP -> {
                    net.minecraftforge.network.NetworkHooks.openScreen(sender, new net.minecraft.world.MenuProvider() {
                        @Override
                        public net.minecraft.network.chat.Component getDisplayName() {
                            return net.minecraft.network.chat.Component.literal("商人");
                        }
                        @Override
                        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player1) {
                            return new com.deltaops.shop.TraderMenu(id, inventory);
                        }
                    });
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
