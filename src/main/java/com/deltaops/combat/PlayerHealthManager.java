/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.combat;

import com.deltaops.DeltaOpsMod;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 將玩家最大血量提升至 100，且不影響視線（抑制低血量紅幕效果）。
 * 使用 AttributeModifier 方式，重生時自動補滿。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class PlayerHealthManager {

    private static final double MAX_HEALTH = 100.0D;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyMaxHealth(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyMaxHealth(player);
    }

    private static void applyMaxHealth(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.setBaseValue(MAX_HEALTH);
        player.setHealth((float) MAX_HEALTH);
    }
}
