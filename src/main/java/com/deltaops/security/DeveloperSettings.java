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
import java.nio.file.Paths;
import java.util.*;

/**
 * 開發者設定管理器：
 * 儲存開發者 UUID 白名單，用於繞過診斷指令權限檢查。
 * Cloudflare Worker URL 已鎖死在 CodeIntegrityValidator 程式碼中，禁止動態修改。
 */
public class DeveloperSettings {

    private static final Path STORAGE = Paths.get("config", DeltaOpsMod.MOD_ID, "developer.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<DeveloperData>() {}.getType();

    private static DeveloperData data = new DeveloperData();

    static {
        load();
    }

    public static void load() {
        try {
            if (Files.exists(STORAGE)) {
                String json = Files.readString(STORAGE, StandardCharsets.UTF_8);
                if (!json.isBlank()) {
                    DeveloperData loaded = GSON.fromJson(json, DATA_TYPE);
                    if (loaded != null) {
                        data = loaded;
                    }
                }
            }
        } catch (IOException ignored) {}

        if (data.developer_uuids == null) {
            data.developer_uuids = new ArrayList<>();
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(STORAGE.getParent());
            Files.writeString(STORAGE, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    public static boolean isDeveloper(UUID playerUuid) {
        if (playerUuid == null) return false;
        return data.developer_uuids.contains(playerUuid.toString());
    }

    public static void addDeveloper(UUID playerUuid) {
        if (playerUuid == null) return;
        String uuidStr = playerUuid.toString();
        if (!data.developer_uuids.contains(uuidStr)) {
            data.developer_uuids.add(uuidStr);
            save();
        }
    }

    public static void removeDeveloper(UUID playerUuid) {
        if (playerUuid == null) return;
        data.developer_uuids.remove(playerUuid.toString());
        save();
    }

    public static List<String> getDeveloperUuids() {
        return Collections.unmodifiableList(data.developer_uuids);
    }

    private static class DeveloperData {
        List<String> developer_uuids = new ArrayList<>();
    }
}
