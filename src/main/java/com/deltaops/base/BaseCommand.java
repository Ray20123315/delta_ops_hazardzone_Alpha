/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.base;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /base 庇護所指令。
 *
 * 用法:
 *   /base               - 查詢庇護所資訊
 *   /base sethome       - 設定庇護所重生點
 *   /base home          - 傳送回庇護所（60 秒冷卻）
 *   /base upgrade <type> - 升級設施（storage / workbench / medical）
 */
@Mod.EventBusSubscriber(modid = com.deltaops.DeltaOpsMod.MOD_ID)
public class BaseCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("base")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    BaseManager.getInfo(player)));
                    return 1;
                })
                .then(Commands.literal("sethome")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BaseManager.setHome(player);
                            return 1;
                        }))
                .then(Commands.literal("home")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BaseManager.teleportToHome(player);
                            return 1;
                        }))
                .then(Commands.literal("upgrade")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String type = ctx.getArgument("type", String.class);
                                    String result = BaseManager.upgrade(player, type.toLowerCase());
                                    ctx.getSource().sendSystemMessage(
                                            net.minecraft.network.chat.Component.literal(result));
                                    return 1;
                                })))
        );
    }
}
