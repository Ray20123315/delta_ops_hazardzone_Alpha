/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.lobby;

import com.deltaops.DeltaOpsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class LobbyProtectionManager {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        // 大廳保護
        if (LobbyArea.isInLobby(player)) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
            player.getFoodData().setExhaustion(0.0F);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // 允許 /kill 指令（OUT_OF_WORLD 傷害來源），方便管理員除錯
        if (event.getSource().getMsgId().equals("outOfWorld")) {
            return;
        }
        if (LobbyArea.isInLobby(player)) {
            event.setCanceled(true);
        }
    }
}
