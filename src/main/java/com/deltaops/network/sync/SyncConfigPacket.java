/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.network.sync;

import com.deltaops.DeltaOpsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 設定檔同步封包（Server→Client）。
 * 當管理員透過 /dt diag sign 或 /dt config sign 手動簽署並載入新設定後，
 * 廣播此封包通知所有線上玩家更新客戶端狀態。
 */
public class SyncConfigPacket {

    private final int priceItemCount;
    private final long timestamp;

    public SyncConfigPacket(int priceItemCount, long timestamp) {
        this.priceItemCount = priceItemCount;
        this.timestamp = timestamp;
    }

    public static void encode(SyncConfigPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.priceItemCount);
        buf.writeLong(packet.timestamp);
    }

    public static SyncConfigPacket decode(FriendlyByteBuf buf) {
        return new SyncConfigPacket(buf.readInt(), buf.readLong());
    }

    public static void handle(SyncConfigPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // 客戶端收到同步通知，可在此更新本地緩存
            DeltaOpsMod.LOGGER.info("📦 [Delta Ops] 收到設定檔同步通知 (物價項目: {}, 時間戳: {})",
                    packet.priceItemCount, packet.timestamp);
        });
        ctx.setPacketHandled(true);
    }

    public int getPriceItemCount() {
        return priceItemCount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
