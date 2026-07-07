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

/**
 * 雲端驗證網路客戶端。
 * 負責與 Cloudflare Worker 進行 JSON 通訊，所有驗證邏輯由 Worker 端判斷。
 *
 * 請求格式：
 *   verify：  {"action":"verify", "hash":"<SHA256>"}
 *   register：{"action":"register", "token":"<SHA256(密碼+SALT)>", "hash":"<SHA256>"}
 *
 * 回應格式：
 *   成功：{"status":"VALID"} 或 {"status":"SUCCESS"}
 *   失敗：{"status":"INVALID"} 或 {"status":"ERROR","message":"..."}
 */
public class NetworkClient {

    /** Cloudflare Worker 驗證網址 */
    private static final String WORKER_URL = "https://delta-ops-validator.ray20123315.workers.dev/";

    /** 連線與讀取超時（毫秒） */
    private static final int TIMEOUT_MS = 5000;

    /**
     * 發送 verify 請求，驗證當前模組 Hash 是否通過官方認證。
     *
     * @param moduleHash 當前模組的 SHA-256 十六進位字串
     * @return true 表示 Worker 回傳 VALID，false 表示未通過或連線失敗
     */
    public static boolean sendVerify(String moduleHash) {
        try {
            String body = "{\"action\":\"verify\",\"hash\":\"" + moduleHash + "\"}";
            String response = doPost(body);

            if (response == null || response.isBlank()) return false;

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String status = json.has("status") ? json.get("status").getAsString() : "";
            return "VALID".equals(status);

        } catch (Exception e) {
            DeltaOpsMod.LOGGER.error("⚠️ [NetworkClient] verify 請求異常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 發送 register 請求，使用管理員密碼進行註冊解鎖。
     * Java 端不判斷密碼對錯，全部交由 Worker 判斷。
     *
     * @param token SHA-256(密碼 + SALT) 的十六進位字串
     * @param moduleHash 當前模組的 SHA-256 十六進位字串
     * @return true 表示 Worker 回傳 SUCCESS，false 表示失敗
     */
    public static boolean sendRegister(String token, String moduleHash) {
        try {
            String body = "{\"action\":\"register\",\"token\":\"" + token + "\",\"hash\":\"" + moduleHash + "\"}";
            String response = doPost(body);

            if (response == null || response.isBlank()) return false;

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String status = json.has("status") ? json.get("status").getAsString() : "";
            return "SUCCESS".equals(status);

        } catch (Exception e) {
            DeltaOpsMod.LOGGER.error("⚠️ [NetworkClient] register 請求異常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 執行 HTTP POST 請求，發送 JSON 內容並回傳回應字串。
     */
    private static String doPost(String jsonBody) {
        HttpURLConnection conn = null;
        try {
            URI uri = new URI(WORKER_URL);
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            // 寫入請求內容
            conn.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));

            // 讀取回應
            int responseCode = conn.getResponseCode();
            InputStream stream = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (stream != null) {
                byte[] resp = stream.readAllBytes();
                return new String(resp, StandardCharsets.UTF_8);
            }
            return "";

        } catch (Exception e) {
            DeltaOpsMod.LOGGER.error("⚠️ [NetworkClient] HTTP 請求失敗: {}", e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
