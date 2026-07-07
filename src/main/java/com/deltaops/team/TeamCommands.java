/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.team;

import com.deltaops.DeltaOpsMod;
import com.deltaops.extraction.ExtractionPointManager;
import com.deltaops.lobby.LobbySquadManager;
// import com.deltaops.lobby.MatchSessionManager; // Class not yet implemented
import com.deltaops.lobby.EconomyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class TeamCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("dt")
                .then(Commands.literal("apply")
                        .then(Commands.argument("team", StringArgumentType.string())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    TeamManager.applyToTeam(player, java.util.UUID.fromString(context.getArgument("team", String.class)));
                                    return 1;
                                })))
                .then(Commands.literal("accept")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(context -> {
                                    ServerPlayer leader = context.getSource().getPlayerOrException();
                                    ServerPlayer applicant = context.getSource().getServer().getPlayerList().getPlayerByName(context.getArgument("player", String.class));
                                    if (applicant != null) {
                                        TeamManager.acceptApplicant(leader, applicant);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(context -> {
                                    ServerPlayer leader = context.getSource().getPlayerOrException();
                                    ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(context.getArgument("player", String.class));
                                    if (target != null) {
                                        TeamManager.kickMember(leader, target);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("leave")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            TeamManager.leaveTeam(player);
                            return 1;
                        }))
                .then(Commands.literal("launch")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("mapName", StringArgumentType.word())
                                .executes(context -> {
                                    ServerPlayer leader = context.getSource().getPlayerOrException();
                                    String mapName = StringArgumentType.getString(context, "mapName");
                                    boolean success = LobbySquadManager.attemptLaunch(leader, mapName);
                                    if (!success) {
                                        leader.sendSystemMessage(Component.literal("§c[Delta Ops] 開始遊戲失敗，請確認隊伍準備狀態與地圖名稱。"));
                                        return 0;
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("extract")
                        .then(Commands.literal("add")
                                .then(Commands.argument("mapName", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String mapName = StringArgumentType.getString(context, "mapName");
                                            if (ExtractionPointManager.registerExtractionPoint(player, mapName)) {
                                                player.sendSystemMessage(Component.literal("§a已為地圖 [" + mapName + "] 設定撤離點。"));
                                                return 1;
                                            }
                                            player.sendSystemMessage(Component.literal("§c未能設定撤離點，請確認地圖名稱是否正確。"));
                                            return 0;
                                        }))))
                .then(Commands.literal("balance")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            long balance = EconomyManager.getBalance(player);
                            player.sendSystemMessage(Component.literal("§e你的哈夫幣餘額：" + balance));
                            return 1;
                        })));
    }
}
