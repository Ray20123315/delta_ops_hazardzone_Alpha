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
 * 當玩家登入伺服器時：
 * 1. 檢查 CodeIntegrityValidator 是否有舊版本升級通知
 * 2. 若代碼完整性驗證失敗，向管理員/開發者發送警告
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class PlayerJoinHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 僅在伺服器端執行
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 雙通道權限檢查：OP Level 2 或開發者 UUID
        boolean isPrivileged = player.hasPermissions(2)
                || (player.getServer() != null
                && player.getServer().getPlayerList().isOp(player.getGameProfile()));

        // ===== 檢查 1：完整性驗證失敗/錯誤 =====
        CodeIntegrityValidator.IntegrityStatus status = CodeIntegrityValidator.getStatus();
        if (status == CodeIntegrityValidator.IntegrityStatus.INVALID
                || status == CodeIntegrityValidator.IntegrityStatus.ERROR) {
            // 僅對有權限的玩家顯示詳細警告
            if (isPrivileged) {
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("§c§m-----------------------------------------------------"));
                player.sendSystemMessage(Component.literal("§c⚠ 核心代碼完整性驗證失敗！"));
                player.sendSystemMessage(Component.literal("§c模組可能已被非法篡改，經濟系統已鎖死。"));
                String errMsg = CodeIntegrityValidator.getLastErrorMessage();
                if (errMsg != null && !errMsg.isBlank()) {
                    player.sendSystemMessage(Component.literal("§7原因: §f" + errMsg));
                }
                player.sendSystemMessage(Component.literal("§c§m-----------------------------------------------------"));
                player.sendSystemMessage(Component.literal(""));
            } else {
                // 一般玩家：只顯示簡短提示
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("§c⚠ Delta Ops 安全模組未通過驗證，部分功能可能受限。"));
                player.sendSystemMessage(Component.literal(""));
            }
            // 驗證失敗時不再檢查升級通知
            return;
        }

        // ===== 檢查 2：舊版本升級通知 =====
        if (!CodeIntegrityValidator.hasUpgradeNotice()) return;

        if (!isPrivileged) return;

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
