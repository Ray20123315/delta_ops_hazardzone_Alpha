/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.network.squad;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 通用封包：服務端通知客戶端開啟指定畫面。
 * screenType: "quest" → QuestScreen
 */
public class ClientboundOpenScreenPacket {
    private final String screenType;

    public ClientboundOpenScreenPacket(String screenType) {
        this.screenType = screenType;
    }

    public static void encode(ClientboundOpenScreenPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.screenType);
    }

    public static ClientboundOpenScreenPacket decode(FriendlyByteBuf buf) {
        return new ClientboundOpenScreenPacket(buf.readUtf());
    }

    public static void handle(ClientboundOpenScreenPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if ("quest".equals(pkt.screenType)) {
                mc.setScreen(new com.deltaops.quest.QuestScreen());
            } else if ("shop".equals(pkt.screenType)) {
                // 商店使用容器式 GUI，由 NetworkHooks.openScreen 處理
            }
        });
        ctx.setPacketHandled(true);
    }
}
