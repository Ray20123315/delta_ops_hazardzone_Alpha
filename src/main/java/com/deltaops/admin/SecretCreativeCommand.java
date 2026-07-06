/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.admin;

import com.deltaops.DeltaOpsMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class SecretCreativeCommand {
    private static final Set<UUID> ALLOWED = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("gamemode")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("mode", GameModeArgument.gameMode())
                        .executes(ctx -> {
                            GameType mode = GameModeArgument.getGameMode(ctx, "mode");
                            if (mode == GameType.CREATIVE) {
                                return 0;
                            }
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            p.setGameMode(mode);
                            return 1;
                        }))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§c用法: /gamemode <模式>"));
                    return 0;
                }));

        d.register(Commands.literal("gm")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("mode", GameModeArgument.gameMode())
                        .executes(ctx -> {
                            GameType mode = GameModeArgument.getGameMode(ctx, "mode");
                            if (mode == GameType.CREATIVE) {
                                return 0;
                            }
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            p.setGameMode(mode);
                            return 1;
                        }))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§c用法: /gm <模式>"));
                    return 0;
                }));

        d.register(Commands.literal("c").requires(src -> src.hasPermission(2)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            UUID id = p.getUUID();
            boolean nowAllowed = ALLOWED.contains(id);
            if (nowAllowed) {
                ALLOWED.remove(id);
                p.setGameMode(GameType.SURVIVAL);
            } else {
                ALLOWED.add(id);
                p.setGameMode(GameType.CREATIVE);
            }
            return 1;
        }));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        try {
            if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE && !ALLOWED.contains(id)) {
                player.setGameMode(GameType.SURVIVAL);
            }
        } catch (Exception ignored) {}
    }
}
