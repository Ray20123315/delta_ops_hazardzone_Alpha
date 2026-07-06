/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.security;

import com.deltaops.DeltaOpsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 設定檔 HMAC 簽名管理器。
 * 用於對 JSON 設定檔（物價表等）產生與驗證 HMAC 簽名，
 * 防止玩家或第三方直接修改檔案內容。
 *
 * 簽名儲存在同目錄下的 .signatures 檔案中。
 */
public class HMACConfigManager {

    private static final String SIGNATURE_FILE_SUFFIX = ".signatures";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SIG_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    /**
     * 對指定的設定檔產生 HMAC 簽名並儲存。
     * @param configPath 設定檔路徑
     */
    public static void signConfig(Path configPath) {
        try {
            if (!Files.exists(configPath)) return;

            String content = Files.readString(configPath, StandardCharsets.UTF_8);
            String signature = SignatureService.generateHMAC(content);

            Path sigFile = getSignaturePath(configPath);
            Map<String, String> signatures = loadSignatures(sigFile);
            signatures.put(configPath.getFileName().toString(), signature);
            saveSignatures(sigFile, signatures);

        } catch (IOException e) {
            DeltaOpsMod.LOGGER.error("無法簽名設定檔: " + configPath, e);
        }
    }

    /**
     * 驗證指定的設定檔簽名是否正確。
     * @param configPath 設定檔路徑
     * @return true 如果簽名驗證通過或不存在簽名
     */
    public static boolean verifyConfig(Path configPath) {
        try {
            if (!Files.exists(configPath)) return true;

            String content = Files.readString(configPath, StandardCharsets.UTF_8);
            Path sigFile = getSignaturePath(configPath);
            Map<String, String> signatures = loadSignatures(sigFile);

            String expectedSignature = signatures.get(configPath.getFileName().toString());
            if (expectedSignature == null) {
                // 尚未簽名，視為合法（首次載入）
                return true;
            }

            return SignatureService.verifyHMAC(content, expectedSignature);

        } catch (IOException e) {
            DeltaOpsMod.LOGGER.error("無法驗證設定檔: " + configPath, e);
            return false;
        }
    }

    /**
     * 移除某個設定檔的簽名記錄（用於管理員重新產生物價表時）。
     */
    public static void clearSignature(Path configPath) {
        Path sigFile = getSignaturePath(configPath);
        Map<String, String> signatures = loadSignatures(sigFile);
        signatures.remove(configPath.getFileName().toString());
        saveSignatures(sigFile, signatures);
    }

    private static Path getSignaturePath(Path configPath) {
        return configPath.getParent().resolve(configPath.getFileName() + SIGNATURE_FILE_SUFFIX);
    }

    private static Map<String, String> loadSignatures(Path sigFile) {
        try {
            if (Files.exists(sigFile)) {
                String json = Files.readString(sigFile, StandardCharsets.UTF_8);
                Map<String, String> loaded = GSON.fromJson(json, SIG_TYPE);
                if (loaded != null) return loaded;
            }
        } catch (IOException ignored) {}
        return new HashMap<>();
    }

    private static void saveSignatures(Path sigFile, Map<String, String> signatures) {
        try {
            Files.createDirectories(sigFile.getParent());
            Files.writeString(sigFile, GSON.toJson(signatures), StandardCharsets.UTF_8);
        } catch (IOException e) {
            DeltaOpsMod.LOGGER.error("無法儲存簽名檔: " + sigFile, e);
        }
    }
}
