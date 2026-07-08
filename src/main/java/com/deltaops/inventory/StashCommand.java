/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import com.deltaops.DeltaOpsMod;
import com.deltaops.lobby.LobbySquadManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/**
 * 倉庫/賣出指令：/dt stash 開啟倉庫
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class StashCommand {

    private static boolean checkReady(ServerPlayer player) {
        if (LobbySquadManager.isPlayerReady(player)) {
            player.sendSystemMessage(Component.literal("§c準備狀態下無法使用倉庫功能！請先取消準備。"));
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        d.register(net.minecraft.commands.Commands.literal("dt")
                .then(net.minecraft.commands.Commands.literal("stash")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (checkReady(player)) return 0;
                            NetworkHooks.openScreen(player, new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("倉庫");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int id, Inventory inv, Player player1) {
                                    return new WarehouseMenu(id, inv, player1);
                                }
                            });
                            return 1;
                        }))
                .then(net.minecraft.commands.Commands.literal("sell")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (checkReady(player)) return 0;
                            long total = WarehouseManager.sellAll(player);
                            player.sendSystemMessage(Component.literal("§a已賣出所有倉庫物品，獲得 " + total + " 哈夫幣！"));
                            return 1;
                        }))
                .then(net.minecraft.commands.Commands.literal("sellgui")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (checkReady(player)) return 0;
                            NetworkHooks.openScreen(player, new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("賣出物品");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int id, Inventory inv, Player player1) {
                                    return new SellMenu(id, inv, player1);
                                }
                            });
                            return 1;
                        })));
    }
}
