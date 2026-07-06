/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.quest;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundQuestDataPacket {
    private final List<QuestData.QuestTemplate> templates;
    private final List<Integer> progresses;

    public ClientboundQuestDataPacket(List<QuestManager.ActiveQuest> quests) {
        this.templates = new ArrayList<>();
        this.progresses = new ArrayList<>();
        for (QuestManager.ActiveQuest aq : quests) {
            templates.add(aq.template());
            progresses.add(aq.currentProgress());
        }
    }

    public static void encode(ClientboundQuestDataPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.templates.size());
        for (int i = 0; i < packet.templates.size(); i++) {
            QuestData.QuestTemplate t = packet.templates.get(i);
            buf.writeUtf(t.id());
            buf.writeUtf(t.title());
            buf.writeUtf(t.description());
            buf.writeUtf(t.type().name());
            buf.writeUtf(t.targetId());
            buf.writeInt(t.requiredCount());
            buf.writeLong(t.reward());
            buf.writeInt(packet.progresses.get(i));
        }
    }

    public static ClientboundQuestDataPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<QuestManager.ActiveQuest> quests = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            String title = buf.readUtf();
            String desc = buf.readUtf();
            QuestData.QuestType type = QuestData.QuestType.valueOf(buf.readUtf());
            String targetId = buf.readUtf();
            int required = buf.readInt();
            long reward = buf.readLong();
            int progress = buf.readInt();
            QuestData.QuestTemplate t = new QuestData.QuestTemplate(id, title, desc, type, targetId, required, reward);
            quests.add(new QuestManager.ActiveQuest(t, progress));
        }
        return new ClientboundQuestDataPacket(quests);
    }

    public static void handle(ClientboundQuestDataPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof QuestScreen screen) {
                List<QuestManager.ActiveQuest> quests = new ArrayList<>();
                for (int i = 0; i < packet.templates.size(); i++) {
                    quests.add(new QuestManager.ActiveQuest(packet.templates.get(i), packet.progresses.get(i)));
                }
                screen.updateQuests(quests);
            }
        });
        ctx.setPacketHandled(true);
    }

    public static void send(ServerPlayer player, List<QuestManager.ActiveQuest> quests) {
        com.deltaops.network.ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ClientboundQuestDataPacket(quests));
    }
}
