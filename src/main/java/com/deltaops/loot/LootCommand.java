/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.loot;

import com.deltaops.DeltaOpsMod;
import com.deltaops.admin.AdminConfigMenu;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class LootCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("dt")
                .then(Commands.literal("admin")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("items").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            NetworkHooks.openScreen(player, new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("物品標籤編輯器");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player1) {
                                    return new AdminItemTaggingMenu(id, inventory);
                                }
                            });
                            player.sendSystemMessage(Component.literal("已開啟物品分類編輯器。"));
                            return 1;
                        }))
                        .then(Commands.literal("config").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            NetworkHooks.openScreen(player, new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("管理員設定");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player1) {
                                    return new AdminConfigMenu(id, inventory);
                                }
                            });
                            player.sendSystemMessage(Component.literal("已開啟管理員設定介面。"));
                            return 1;
                        }))));
    }
}
