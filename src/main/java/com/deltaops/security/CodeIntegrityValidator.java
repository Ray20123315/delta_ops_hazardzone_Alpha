/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.security;

import com.deltaops.DeltaOpsMod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * 核心代碼防護（第一層防線）：
 * 1. 計算當前運行模組的 SHA-256 雜湊
 * 2. 透過 Cloudflare Worker（硬編碼常數網址）進行 API 驗證
 * 3. 開發環境（Dev Mode）自動跳過 HTTP 請求
 *
 * Worker 回傳 JSON 格式：
 *   VALID + 最新版：{"status":"VALID","version":"1.1.0","isLatest":true,"message":"..."}
 *   VALID + 舊版：  {"status":"VALID","version":"1.0.0","isLatest":false,"message":"..."}
 *   INVALID：       HTTP 403 {"status":"INVALID"}
 */
public class CodeIntegrityValidator {

    // ========== 硬編碼常數（禁止從外部修改） ==========
    /** Cloudflare Worker 驗證網址 — 鎖死在代碼最深處 */
    private static final String VERIFICATION_URL = "https://delta-ops-validator.ray20123315.workers.dev/";
    /** 通訊安全 Token */
    private static final String COMMUNICATION_TOKEN = "DeltaOps_Secret_Communication_Token_2026";

    // ========== 驗證結果狀態 ==========
    public enum IntegrityStatus {
        PENDING,   // 尚未驗證
        VALID,     // 驗證通過
        INVALID,   // 驗證失敗（可能被篡改）
        ERROR      // 連線錯誤（無法連線到 Worker）
    }

    private static IntegrityStatus integrityStatus = IntegrityStatus.PENDING;
    private static String lastErrorMessage = "";
    private static String verifiedVersion = "";
    private static boolean hasUpgrade = false;
    private static String upgradeMessage = "";

    /**
     * 非同步啟動完整性驗證。
     * 開發環境（Dev Mode）下自動跳過 HTTP 請求。
     */
    public static void startValidation() {
        CompletableFuture.runAsync(() -> {
            try {
                // === Dev Mode 檢查 ===
                if (isDevelopmentEnvironment()) {
                    integrityStatus = IntegrityStatus.VALID;
                    DeltaOpsMod.LOGGER.info("ℹ️ [Delta Ops] 開發環境模式，跳過完整性驗證。");
                    return;
                }

                // 1. 計算當前模組的 SHA-256
                String moduleHash = computeModuleHash();
                if (moduleHash.isEmpty()) {
                    integrityStatus = IntegrityStatus.ERROR;
                    lastErrorMessage = "無法計算模組 Hash";
                    return;
                }

                // 2. 向 Cloudflare Worker 發送 POST 請求（非同步）
                boolean valid = verifyWithCloudflare(moduleHash);
                if (valid) {
                    integrityStatus = IntegrityStatus.VALID;
                } else {
                    integrityStatus = IntegrityStatus.INVALID;
                    DeltaOpsMod.LOGGER.warn("⚠️ ╔══════════════════════════════════════════════╗");
                    DeltaOpsMod.LOGGER.warn("⚠️ ║  核心代碼完整性驗證失敗！可能已被篡改！    ║");
                    DeltaOpsMod.LOGGER.warn("⚠️ ╚══════════════════════════════════════════════╝");
                    DeltaOpsMod.LOGGER.warn("⚠️ [Delta Ops] " + lastErrorMessage);
                }

            } catch (Exception e) {
                integrityStatus = IntegrityStatus.ERROR;
                lastErrorMessage = e.getMessage();
                DeltaOpsMod.LOGGER.error("⚠️ [Delta Ops] 完整性驗證異常: " + e.getMessage());
            }
        });
    }

    /**
     * 判斷是否為開發環境（非打包後的生產環境 JAR）。
     * 透過檢查 Class 是否來自檔案系統而非 JAR 來判斷。
     */
    private static boolean isDevelopmentEnvironment() {
        try {
            String path = DeltaOpsMod.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            return path != null && !path.toLowerCase().contains(".jar");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 計算當前運行模組 jar 的 SHA-256。
     */
    private static String computeModuleHash() {
        try {
            String className = "/" + DeltaOpsMod.class.getName().replace('.', '/') + ".class";
            try (InputStream is = DeltaOpsMod.class.getResourceAsStream(className)) {
                if (is == null) return "";
                byte[] buffer = new byte[8192];
                int bytesRead;
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
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
            DeltaOpsMod.LOGGER.error("計算模組 Hash 失敗", e);
            return "";
        }
    }

    /**
     * 透過 Cloudflare Worker 驗證模組 Hash。
     * 新版協議：POST JSON 含 hash + token，解析回傳 JSON。
     */
    private static boolean verifyWithCloudflare(String moduleHash) {
        try {
            // 準備請求 JSON（含 token）
            String payload = "{\"hash\":\"" + moduleHash + "\",\"token\":\"" + COMMUNICATION_TOKEN + "\"}";

            URI uri = new URI(VERIFICATION_URL);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // 寫入請求內容
            conn.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));

            // 讀取回應
            int responseCode = conn.getResponseCode();

            // 嘗試讀取回應 body（不論成功或失敗）
            String responseBody = "";
            try {
                InputStream stream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (stream != null) {
                    byte[] resp = stream.readAllBytes();
                    responseBody = new String(resp, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) { }

            // 解析 JSON 回應（如果有 body）
            String status = "";
            String reason = "";
            if (!responseBody.isBlank()) {
                try {
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    if (json.has("status")) status = json.get("status").getAsString();
                    if (json.has("reason")) reason = json.get("reason").getAsString();
                    if (reason.isBlank() && json.has("message")) reason = json.get("message").getAsString();
                } catch (Exception ignored) { }
            }

            // HTTP 200 + VALID → 驗證通過
            if (responseCode == 200 && "VALID".equals(status)) {
                try {
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    if (json.has("version")) verifiedVersion = json.get("version").getAsString();
                    if (json.has("isLatest") && !json.get("isLatest").getAsBoolean()) {
                        hasUpgrade = true;
                        if (json.has("message")) upgradeMessage = json.get("message").getAsString();
                        DeltaOpsMod.LOGGER.warn("⚠️ ╔══════════════════════════════════════════════╗");
                        DeltaOpsMod.LOGGER.warn("⚠️ ║  偵測到舊版模組！請盡快升級！              ║");
                        DeltaOpsMod.LOGGER.warn("⚠️ ╠══════════════════════════════════════════════╣");
                        DeltaOpsMod.LOGGER.warn("⚠️ ║  目前版本: " + padVersion(verifiedVersion, 26) + "║");
                        DeltaOpsMod.LOGGER.warn("⚠️ ╚══════════════════════════════════════════════╝");
                        if (!upgradeMessage.isBlank()) {
                            for (String line : upgradeMessage.split("\n")) {
                                DeltaOpsMod.LOGGER.warn("⚠️ [升級通知] " + line);
                            }
                        }
                    } else {
                        DeltaOpsMod.LOGGER.info("✅ [Delta Ops] 模組完整性驗證通過 (v{})。", verifiedVersion);
                    }
                } catch (Exception ignored) { }
                return true;
            }

            // HTTP 403 + INVALID → 驗證失敗（非法修改）
            if (responseCode == 403 || "INVALID".equals(status)) {
                lastErrorMessage = "Cloudflare Worker 拒絕驗證";
                if (!reason.isBlank()) lastErrorMessage += " (" + reason + ")";
                lastErrorMessage += " (Hash: " + moduleHash + ")";
                DeltaOpsMod.LOGGER.error("⚠️ [Delta Ops] " + lastErrorMessage);
                return false;
            }

            // HTTP 400 + ERROR → 請求格式錯誤
            if (responseCode == 400 || "ERROR".equals(status)) {
                lastErrorMessage = "Cloudflare Worker 請求格式錯誤";
                if (!reason.isBlank()) lastErrorMessage += " (" + reason + ")";
                DeltaOpsMod.LOGGER.error("⚠️ [Delta Ops] " + lastErrorMessage);
                return false;
            }

            // 其他狀態 → 連線/伺服器錯誤
            lastErrorMessage = "Cloudflare Worker 回傳異常 (HTTP " + responseCode + ")";
            if (!reason.isBlank()) lastErrorMessage += " (" + reason + ")";
            DeltaOpsMod.LOGGER.warn("⚠️ [Delta Ops] " + lastErrorMessage);
            return false;

        } catch (Exception e) {
            lastErrorMessage = "Cloudflare Worker 連線失敗: " + e.getMessage();
            DeltaOpsMod.LOGGER.warn("⚠️ [Delta Ops] " + lastErrorMessage);
            return false;
        }
    }

    private static String padVersion(String version, int totalLen) {
        if (version == null) version = "?";
        int padding = totalLen - version.length();
        if (padding > 0) {
            return version + " ".repeat(padding);
        }
        return version;
    }

    // ========== 公開 Getter ==========

    public static IntegrityStatus getStatus() {
        return integrityStatus;
    }

    public static String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /** 完整性驗證是否通過（通過或 PENDING 都視為合法，防止剛啟動時誤鎖） */
    public static boolean isIntegrityValid() {
        return integrityStatus == IntegrityStatus.VALID || integrityStatus == IntegrityStatus.PENDING;
    }

    /** 是否有舊版本升級通知 */
    public static boolean hasUpgradeNotice() {
        return hasUpgrade;
    }

    /** 取得升級通知訊息全文 */
    public static String getLatestUpgradeMessage() {
        return upgradeMessage;
    }

    /** 取得已驗證的版本號 */
    public static String getVerifiedVersion() {
        return verifiedVersion;
    }
}
