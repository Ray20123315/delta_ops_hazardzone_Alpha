package com.deltaops.quest;

import com.deltaops.DeltaOpsMod;
import com.deltaops.network.ModNetwork;
import com.deltaops.network.squad.ClientboundOpenScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class QuestCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("dt")
                .then(Commands.literal("quests")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ModNetwork.CHANNEL.send(
                                    PacketDistributor.PLAYER.with(() -> player),
                                    new ClientboundOpenScreenPacket("quest"));
                            return 1;
                        })));
    }
}
