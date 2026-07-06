/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.security;

import com.deltaops.DeltaOpsMod;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 簽名服務：提供 HMAC-SHA256 簽名與 SHA-256 雜湊計算。
 * 用於設定檔防篡改驗證與核心代碼防護。
 */
public class SignatureService {

    // 混淆的 SECRET_KEY（在程式碼深處定義，不易被直接發現）
    private static final String SECRET_KEY = obfuscateKey();

    private static String obfuscateKey() {
        // 透過多層拼接混淆，避免直接被靜態分析找到
        String p1 = "D3lt4";
        String p2 = "Ops";
        String p3 = "H4z4rd";
        String p4 = "Z0n3";
        String p5 = "S3cr3t";
        String p6 = "K3y";
        return p1 + "_" + p2 + "_" + p3 + "_" + p4 + "_" + p5 + "_" + p6;
    }

    /**
     * 計算 JSON 資料的 HMAC-SHA256 簽名。
     * @param jsonData JSON 字串
     * @return Base64 編碼的 HMAC 簽名
     */
    public static String generateHMAC(String jsonData) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hmacBytes = mac.doFinal(jsonData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            DeltaOpsMod.LOGGER.error("Failed to generate HMAC", e);
            return "";
        }
    }

    /**
     * 驗證 JSON 資料的 HMAC 簽名是否正確。
     * @param jsonData JSON 字串
     * @param expectedSignature 預期的簽名（Base64）
     * @return true 如果簽名匹配
     */
    public static boolean verifyHMAC(String jsonData, String expectedSignature) {
        if (expectedSignature == null || expectedSignature.isBlank()) return false;
        String computed = generateHMAC(jsonData);
        return computed.equals(expectedSignature);
    }

    /**
     * 計算 SHA-256 雜湊值。
     * @param data 字串資料
     * @return 16 進制 SHA-256 字串
     */
    public static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            DeltaOpsMod.LOGGER.error("Failed to compute SHA-256", e);
            return "";
        }
    }

    /**
     * 計算檔案的 SHA-256 雜湊值。
     * @param fileBytes 檔案內容的二進位資料
     * @return 16 進制 SHA-256 字串
     */
    public static String sha256File(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            DeltaOpsMod.LOGGER.error("Failed to compute file SHA-256", e);
            return "";
        }
    }
}
