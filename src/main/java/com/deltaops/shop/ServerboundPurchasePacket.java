/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.shop;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundPurchasePacket {
    private final int slotIndex;

    public ServerboundPurchasePacket(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public static void encode(ServerboundPurchasePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.slotIndex);
    }

    public static ServerboundPurchasePacket decode(FriendlyByteBuf buf) {
        return new ServerboundPurchasePacket(buf.readInt());
    }

    public static void handle(ServerboundPurchasePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.containerMenu instanceof TraderMenu menu) {
                menu.purchase(packet.slotIndex);
            }
        });
        ctx.setPacketHandled(true);
    }
}
