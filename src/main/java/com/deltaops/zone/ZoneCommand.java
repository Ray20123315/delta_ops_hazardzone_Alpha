/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.zone;

import com.deltaops.DeltaOpsMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class ZoneCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        var pos1 = Commands.literal("pos1").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            ZoneSelectionManager.setPos1(p.getUUID(), p.blockPosition());
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("pos1 set."));
            return 1;
        });

        var pos2 = Commands.literal("pos2").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            ZoneSelectionManager.setPos2(p.getUUID(), p.blockPosition());
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("pos2 set."));
            return 1;
        });

        var create = Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("priority", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    String pr = StringArgumentType.getString(ctx, "priority").toUpperCase();
                                    ZoneSelectionManager.SearchPriority priority;
                                    try {
                                        priority = ZoneSelectionManager.SearchPriority.valueOf(pr);
                                    } catch (IllegalArgumentException e) {
                                        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("invalid priority"));
                                        return 0;
                                    }
                                    ZoneSelectionManager.saveZone(name, priority, p.getUUID());
                                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Zone saved (無綁定地圖)。"));
                                    return 1;
                                })
                                .then(Commands.argument("mapId", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            String pr = StringArgumentType.getString(ctx, "priority").toUpperCase();
                                            String mapId = StringArgumentType.getString(ctx, "mapId");
                                            ZoneSelectionManager.SearchPriority priority;
                                            try {
                                                priority = ZoneSelectionManager.SearchPriority.valueOf(pr);
                                            } catch (IllegalArgumentException e) {
                                                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("invalid priority"));
                                                return 0;
                                            }
                                            ZoneSelectionManager.saveZone(name, priority, p.getUUID(), mapId);
                                            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Zone saved for map: " + mapId));
                                            return 1;
                                        }))));

        var generate = Commands.literal("generate").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!(p.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("僅限伺服器端使用。"));
                return 0;
            }
            com.deltaops.container.LootContainerSpawner.spawnFromZones(serverLevel);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("已根據區域設定生成戰利品容器。"));
            return 1;
        });

        var zone = Commands.literal("zone").then(pos1).then(pos2).then(create).then(generate);
        var root = Commands.literal("dt").then(zone);
        d.register(root);
    }
}
