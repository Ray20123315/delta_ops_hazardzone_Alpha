package com.deltaops.admin;

import com.deltaops.extraction.ExtractionPointManager;
import com.deltaops.lobby.EconomyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundAdminConfigActionPacket {
    public enum Action {
        SET_EXTRACTION_POINT,
        RELOAD_PRICES,
        SET_ITEM_PRICE,
        SET_SECURE_BOX_LEVEL,
        SET_DEATH_DROP_RULE,
        SET_EXTRACTION_TIMER,
        SET_REWARD_MULTIPLIER
    }

    private final Action action;
    private final String mapName;
    private final String itemId;
    private final long price;

    public ServerboundAdminConfigActionPacket(Action action, String mapName) {
        this(action, mapName, "", 0L);
    }

    public ServerboundAdminConfigActionPacket(Action action, String mapName, String itemId, long price) {
        this.action = action;
        this.mapName = mapName;
        this.itemId = itemId;
        this.price = price;
    }

    public static void encode(ServerboundAdminConfigActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.mapName == null ? "" : packet.mapName);
        buf.writeUtf(packet.itemId == null ? "" : packet.itemId);
        buf.writeLong(packet.price);
    }

    public static ServerboundAdminConfigActionPacket decode(FriendlyByteBuf buf) {
        return new ServerboundAdminConfigActionPacket(buf.readEnum(Action.class), buf.readUtf(), buf.readUtf(), buf.readLong());
    }

    public static void handle(ServerboundAdminConfigActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            if (packet.action == Action.SET_EXTRACTION_POINT) {
                // 僅管理員（op）可以設置撤離點
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c僅管理員可以設置撤離點。"));
                    return;
                }
                ExtractionPointManager.registerExtractionPoint(player, packet.mapName);
            } else if (packet.action == Action.RELOAD_PRICES) {
                EconomyManager.reloadPrices();
            } else if (packet.action == Action.SET_ITEM_PRICE) {
                EconomyManager.setItemPrice(packet.itemId, packet.price);
            } else if (packet.action == Action.SET_SECURE_BOX_LEVEL) {
                int level = (int) Math.max(1, Math.min(4, packet.price));
                com.deltaops.securebox.SecureBoxCapabilityManager.setSecureBoxLevel(player, level);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("安全箱等級已設為 " + level));
            } else if (packet.action == Action.SET_DEATH_DROP_RULE) {
                String rule = packet.mapName; // 服用 mapName 欄位存放規則
                com.deltaops.config.ModConfig.setDeathDropRule(rule);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("死亡掉落規則已設為: " + rule));
            } else if (packet.action == Action.SET_EXTRACTION_TIMER) {
                int seconds = (int) Math.max(5, Math.min(120, packet.price));
                com.deltaops.config.ModConfig.setExtractionTimer(seconds);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("撤離倒數已設為 " + seconds + " 秒"));
            } else if (packet.action == Action.SET_REWARD_MULTIPLIER) {
                double mult = Math.max(0.1, Math.min(10.0, packet.price / 100.0));
                com.deltaops.config.ModConfig.setRewardMultiplier(mult);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("獎勵倍率已設為 " + mult + "x"));
            }
        });
        ctx.setPacketHandled(true);
    }
}
