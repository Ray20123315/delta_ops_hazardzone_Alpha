/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.weapon;

import com.deltaops.DeltaOpsMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 武器改裝檯已移除，改為技能系統。
 * 請使用 /dt skills 開啟技能面板。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class WeaponWorkbenchCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("dt")
                .then(Commands.literal("workbench")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal("§c武器改裝檯已移除！請使用 §6/dt skills §c開啟技能面板。"));
                            return 1;
                        })));
    }
}
