/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.weapon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundWeaponWorkbenchPacket {
    public enum Action { ATTACH, DETACH_ALL }

    private final Action action;

    public ServerboundWeaponWorkbenchPacket(Action action) {
        this.action = action;
    }

    public static void encode(ServerboundWeaponWorkbenchPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
    }

    public static ServerboundWeaponWorkbenchPacket decode(FriendlyByteBuf buf) {
        return new ServerboundWeaponWorkbenchPacket(buf.readEnum(Action.class));
    }

    public static void handle(ServerboundWeaponWorkbenchPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof WeaponWorkbenchMenu workbench)) return;

            if (packet.action == Action.ATTACH) {
                ItemStack result = workbench.attachAttachment();
                if (!result.isEmpty()) {
                    // 將改裝後的武器放回武器槽
                    // (WeaponWorkbenchMenu 內部已處理)
                }
            } else if (packet.action == Action.DETACH_ALL) {
                // 卸載所有配件
                for (WeaponAttachment.AttachmentType type : WeaponAttachment.AttachmentType.values()) {
                    workbench.removeAttachment(type);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
