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
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.io.InputStream;
import java.security.MessageDigest;

/**
 * 狀態攔截與雲端驗證系統。
 *
 * 核心設計：
 * - {@link #isVerified} 全域狀態變數，標記模組是否通過驗證
 * - 啟動階段（FMLCommonSetupEvent）自動發送 verify 請求
 * - 玩家進入世界時（PlayerLoggedInEvent）若未驗證則再次檢查
 * - 管理員可透過 /logintoken <密碼> 進行註冊解鎖
 *
 * 驗證邏輯不可寫死在 Java 中，完全依賴 Cloudflare Worker 回傳結果。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class VerificationManager {

    /** 全域驗證狀態：true = 已通過驗證，false = 未驗證或驗證失敗 */
    public static boolean isVerified = false;

    /** 開發環境時使用的測試 Hash */
    private static final String DEV_TEST_HASH = "DEV_TEST_HASH";

    /** 當前模組的 SHA-256 Hash（快取，避免重複計算） */
    private static String moduleHash = null;

    /** 是否為開發環境 */
    private static boolean devEnvironment = false;

    // ========== 公開方法 ==========

    /**
     * 取得當前模組的 SHA-256 Hash。
     * 開發環境直接回傳 DEV_TEST_HASH。
     */
    public static String getModuleHash() {
        if (moduleHash == null) {
            moduleHash = computeModuleHash();
        }
        return moduleHash;
    }

    // ========== 事件監聽 ==========

    /**
     * 啟動階段：計算模組 Hash 並發送 verify 請求。
     * 此方法由 DeltaOpsMod 建構子中的 modEventBus 註冊。
     */
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        DeltaOpsMod.LOGGER.info("🔐 [VerificationManager] 啟動雲端驗證流程...");

        // 檢查是否為開發環境
        devEnvironment = isDevelopmentEnvironment();
        if (devEnvironment) {
            moduleHash = DEV_TEST_HASH;
            DeltaOpsMod.LOGGER.info("🔐 [VerificationManager] 開發環境模式，使用 DEV_TEST_HASH");
        } else {
            moduleHash = getModuleHash();
        }

        // 發送 verify 請求
        boolean result = NetworkClient.sendVerify(moduleHash);
        isVerified = result;

        if (result) {
            DeltaOpsMod.LOGGER.info("✅ [VerificationManager] 啟動驗證通過 (hash: {})", moduleHash);
        } else {
            DeltaOpsMod.LOGGER.warn("⚠️ [VerificationManager] 啟動驗證失敗，模組功能將受限");
        }
    }

    /**
     * 玩家登入事件：若尚未驗證則再次發送請求。
     * 若驗證失敗，在聊天框發送警告提示並鎖定模組功能。
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 已驗證 → 直接放行，不再發送請求
        if (isVerified) {
            return;
        }

        // 未驗證 → 再次嘗試 verify
        DeltaOpsMod.LOGGER.info("🔐 [VerificationManager] 玩家 {} 登入，重新驗證...", player.getName().getString());
        boolean result = NetworkClient.sendVerify(moduleHash != null ? moduleHash : DEV_TEST_HASH);

        if (result) {
            isVerified = true;
            DeltaOpsMod.LOGGER.info("✅ [VerificationManager] 驗證通過，功能已解鎖");
        } else {
            // 依然失敗 → 發送聊天警告
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§c§m-----------------------------------------------------"));
            player.sendSystemMessage(Component.literal("§c⚠ 模組驗證失敗！"));
            player.sendSystemMessage(Component.literal("§7請聯絡開發者或輸入 §f/dt login <密碼> §7解鎖模組功能。"));
            player.sendSystemMessage(Component.literal("§c§m-----------------------------------------------------"));
            player.sendSystemMessage(Component.literal(""));
            DeltaOpsMod.LOGGER.warn("⚠️ [VerificationManager] 玩家 {} 驗證失敗，功能鎖定", player.getName().getString());
        }
    }

    // ========== 輔助方法 ==========

    /**
     * 計算當前運行模組 jar 的 SHA-256。
     */
    private static String computeModuleHash() {
        try {
            String className = "/" + DeltaOpsMod.class.getName().replace('.', '/') + ".class";
            try (InputStream is = DeltaOpsMod.class.getResourceAsStream(className)) {
                if (is == null) return DEV_TEST_HASH;
                byte[] buffer = new byte[8192];
                int bytesRead;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
                byte[] hash = digest.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            }
        } catch (Exception e) {
            DeltaOpsMod.LOGGER.error("⚠️ [VerificationManager] 計算模組 Hash 失敗", e);
            return DEV_TEST_HASH;
        }
    }

    /**
     * 判斷是否為開發環境（非打包後的生產環境 JAR）。
     */
    private static boolean isDevelopmentEnvironment() {
        try {
            String path = DeltaOpsMod.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            return path != null && !path.toLowerCase().contains(".jar");
        } catch (Exception e) {
            return false;
        }
    }
}
