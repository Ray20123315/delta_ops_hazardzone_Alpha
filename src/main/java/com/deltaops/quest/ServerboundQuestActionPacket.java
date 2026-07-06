/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.quest;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class ServerboundQuestActionPacket {
    public enum Action { REFRESH, CLAIM }

    private final Action action;

    public ServerboundQuestActionPacket(Action action) {
        this.action = action;
    }

    public static void encode(ServerboundQuestActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
    }

    public static ServerboundQuestActionPacket decode(FriendlyByteBuf buf) {
        return new ServerboundQuestActionPacket(buf.readEnum(Action.class));
    }

    public static void handle(ServerboundQuestActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (packet.action == Action.REFRESH) {
                // 確保有活躍任務
                List<QuestManager.ActiveQuest> active = QuestManager.getActiveQuests(player);
                if (active.isEmpty()) {
                    QuestManager.assignDailyQuests(player);
                    active = QuestManager.getActiveQuests(player);
                }
                // 回傳任務資料給客戶端
                ClientboundQuestDataPacket.send(player, active);
            }
        });
        ctx.setPacketHandled(true);
    }
}
