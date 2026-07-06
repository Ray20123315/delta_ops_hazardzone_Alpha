package com.deltaops.network;

import com.deltaops.DeltaOpsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String VER = "1.0";
    private static int id = 0;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DeltaOpsMod.MOD_ID, "main"),
            () -> VER, VER::equals, VER::equals);

    public static void register() {
        CHANNEL.registerMessage(id++, ServerboundOpenSecureBoxPacket.class, ServerboundOpenSecureBoxPacket::encode, ServerboundOpenSecureBoxPacket::decode, ServerboundOpenSecureBoxPacket::handle);
        CHANNEL.registerMessage(id++, ClientboundMatchStatusPacket.class, ClientboundMatchStatusPacket::encode, ClientboundMatchStatusPacket::decode, ClientboundMatchStatusPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.loot.ServerboundSaveItemTagPacket.class, com.deltaops.loot.ServerboundSaveItemTagPacket::encode, com.deltaops.loot.ServerboundSaveItemTagPacket::decode, com.deltaops.loot.ServerboundSaveItemTagPacket::handle);
        // squad packets
        CHANNEL.registerMessage(id++, com.deltaops.network.squad.ServerboundSquadActionPacket.class, com.deltaops.network.squad.ServerboundSquadActionPacket::encode, com.deltaops.network.squad.ServerboundSquadActionPacket::decode, com.deltaops.network.squad.ServerboundSquadActionPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.network.squad.ClientboundShowRequestScreenPacket.class, com.deltaops.network.squad.ClientboundShowRequestScreenPacket::encode, com.deltaops.network.squad.ClientboundShowRequestScreenPacket::decode, com.deltaops.network.squad.ClientboundShowRequestScreenPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.network.squad.ServerboundHandleRequestPacket.class, com.deltaops.network.squad.ServerboundHandleRequestPacket::encode, com.deltaops.network.squad.ServerboundHandleRequestPacket::decode, com.deltaops.network.squad.ServerboundHandleRequestPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.network.squad.ClientboundOpenSquadScreenPacket.class, com.deltaops.network.squad.ClientboundOpenSquadScreenPacket::encode, com.deltaops.network.squad.ClientboundOpenSquadScreenPacket::decode, com.deltaops.network.squad.ClientboundOpenSquadScreenPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.network.squad.ClientboundSquadStatusPacket.class, com.deltaops.network.squad.ClientboundSquadStatusPacket::encode, com.deltaops.network.squad.ClientboundSquadStatusPacket::decode, com.deltaops.network.squad.ClientboundSquadStatusPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.network.squad.ClientboundOpenScreenPacket.class, com.deltaops.network.squad.ClientboundOpenScreenPacket::encode, com.deltaops.network.squad.ClientboundOpenScreenPacket::decode, com.deltaops.network.squad.ClientboundOpenScreenPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.admin.ServerboundAdminConfigActionPacket.class, com.deltaops.admin.ServerboundAdminConfigActionPacket::encode, com.deltaops.admin.ServerboundAdminConfigActionPacket::decode, com.deltaops.admin.ServerboundAdminConfigActionPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.shop.ServerboundPurchasePacket.class, com.deltaops.shop.ServerboundPurchasePacket::encode, com.deltaops.shop.ServerboundPurchasePacket::decode, com.deltaops.shop.ServerboundPurchasePacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.weapon.ServerboundWeaponWorkbenchPacket.class, com.deltaops.weapon.ServerboundWeaponWorkbenchPacket::encode, com.deltaops.weapon.ServerboundWeaponWorkbenchPacket::decode, com.deltaops.weapon.ServerboundWeaponWorkbenchPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.quest.ServerboundQuestActionPacket.class, com.deltaops.quest.ServerboundQuestActionPacket::encode, com.deltaops.quest.ServerboundQuestActionPacket::decode, com.deltaops.quest.ServerboundQuestActionPacket::handle);
        CHANNEL.registerMessage(id++, com.deltaops.quest.ClientboundQuestDataPacket.class, com.deltaops.quest.ClientboundQuestDataPacket::encode, com.deltaops.quest.ClientboundQuestDataPacket::decode, com.deltaops.quest.ClientboundQuestDataPacket::handle);
        // sync packets
        CHANNEL.registerMessage(id++, com.deltaops.network.sync.SyncConfigPacket.class, com.deltaops.network.sync.SyncConfigPacket::encode, com.deltaops.network.sync.SyncConfigPacket::decode, com.deltaops.network.sync.SyncConfigPacket::handle);
    }
}