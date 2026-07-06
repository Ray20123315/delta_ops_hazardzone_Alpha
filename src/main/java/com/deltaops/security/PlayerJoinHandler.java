/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.security;

import com.deltaops.DeltaOpsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家登入安全與版本檢查監聽器。
 * 當玩家登入伺服器時，檢查 CodeIntegrityValidator 是否有舊版本升級通知。
 * 若玩家符合雙通道權限（OP 或 deltaops.developer 節點），則發送升級提示。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class PlayerJoinHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 僅在伺服器端執行
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 檢查是否有舊版本升級通知
        if (!CodeIntegrityValidator.hasUpgradeNotice()) return;

        // 雙通道權限檢查：OP 或 deltaops.developer 權限節點
        boolean canSeeNotice = player.hasPermissions(2)
                || player.getServer() != null
                && player.getServer().getPlayerList().isOp(player.getGameProfile());

        if (!canSeeNotice) return;

        // 發送紅色標題提示
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§c§m-----------------------------------------------------"));
        player.sendSystemMessage(Component.literal("§c⚠ 偵測到舊版模組！"));
        player.sendSystemMessage(Component.literal("§c§m-----------------------------------------------------"));

        // 將 Worker 回傳的多行升級訊息逐行以黃色字體發送
        String message = CodeIntegrityValidator.getLatestUpgradeMessage();
        if (message != null && !message.isBlank()) {
            for (String line : message.split("\n")) {
                player.sendSystemMessage(Component.literal("§e" + line));
            }
        }

        player.sendSystemMessage(Component.literal("§c§m-----------------------------------------------------"));
        player.sendSystemMessage(Component.literal(""));
    }
}
