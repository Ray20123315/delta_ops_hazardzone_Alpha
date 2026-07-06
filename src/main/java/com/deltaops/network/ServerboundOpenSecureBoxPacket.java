package com.deltaops.network;

import com.deltaops.securebox.SecureBoxCapabilityManager;
import com.deltaops.securebox.SecureBoxMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class ServerboundOpenSecureBoxPacket {
    public ServerboundOpenSecureBoxPacket() {
    }

    public static void encode(ServerboundOpenSecureBoxPacket packet, FriendlyByteBuf buffer) {
    }

    public static ServerboundOpenSecureBoxPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundOpenSecureBoxPacket();
    }

    public static void handle(ServerboundOpenSecureBoxPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.level().isClientSide) {
                return;
            }

            // 檢查安全箱是否已解鎖
            if (!com.deltaops.securebox.SecureBoxCapabilityManager.isUnlocked(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c⚠️ 你尚未解鎖安全箱！請前往商店購買。"));
                return;
            }

            var handler = com.deltaops.securebox.SecureBoxCapabilityManager.getSecureBoxHandler(player);
            NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return net.minecraft.network.chat.Component.literal("Secure Box");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player playerEntity) {
                    return new com.deltaops.securebox.SecureBoxMenu(id, inventory, handler);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
