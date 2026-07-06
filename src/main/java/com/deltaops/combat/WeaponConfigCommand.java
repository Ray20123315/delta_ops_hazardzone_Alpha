/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.combat;

import com.deltaops.DeltaOpsMod;
import com.deltaops.screen.ModMenuTypes;
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
public class WeaponConfigCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("dt")
                .then(Commands.literal("weapons")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            NetworkHooks.openScreen(player, new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("槍械設定");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player1) {
                                    return new WeaponConfigMenu(id, inventory);
                                }
                            });
                            return 1;
                        })));
    }
}
