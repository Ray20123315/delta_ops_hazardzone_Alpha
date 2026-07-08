/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundMatchStatusPacket {
    private final int queuedPlayers;
    private final boolean matchOpen;

    public ClientboundMatchStatusPacket(int queuedPlayers, boolean matchOpen) {
        this.queuedPlayers = queuedPlayers;
        this.matchOpen = matchOpen;
    }

    public static void encode(ClientboundMatchStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.queuedPlayers);
        buffer.writeBoolean(packet.matchOpen);
    }

    public static ClientboundMatchStatusPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundMatchStatusPacket(buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(ClientboundMatchStatusPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.deltaops.client.matchmaking.MatchmakingScreen screen) {
                screen.updateQueueStatus(packet.queuedPlayers, packet.matchOpen);
            }
            if (mc.screen instanceof com.deltaops.client.squad.SquadMainScreen screen) {
                screen.updateQueueStatus(packet.queuedPlayers, packet.matchOpen);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
