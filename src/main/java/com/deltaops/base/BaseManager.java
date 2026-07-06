/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.base;

import com.deltaops.DeltaOpsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基地/庇護所系統（Base/Shelter System）。
 * 每個玩家可擁有一個庇護所，包含基礎建設等級。
 * 庇護所提供：儲物箱、裝備維修、重生點設定等功能。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class BaseManager {

    private static final Path STORAGE_PATH = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
            .resolve(DeltaOpsMod.MOD_ID).resolve("bases.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Map<UUID, BaseData>>() {}.getType();

    private static final Map<UUID, BaseData> playerBases = new ConcurrentHashMap<>();

    static {
        loadAll();
    }

    /** 庇護所數據 */
    public static class BaseData {
        public int storageLevel = 1;        // 儲物等級 (1~5)
        public int workbenchLevel = 1;      // 工作台等級 (1~3)
        public int medicalLevel = 1;        // 醫療等級 (1~3)
        public String homeWorld = "";       // 重生點所在世界
        public int homeX, homeY, homeZ;     // 重生點座標
        public boolean hasHomeSet = false;
    }

    /**
     * 取得玩家庇護所數據。
     */
    public static BaseData getOrCreate(Player player) {
        return playerBases.computeIfAbsent(player.getUUID(), k -> new BaseData());
    }

    /**
     * 設定庇護所重生點。
     */
    public static void setHome(ServerPlayer player) {
        BaseData data = getOrCreate(player);
        data.homeWorld = player.level().dimension().location().toString();
        data.homeX = (int) player.getX();
        data.homeY = (int) player.getY();
        data.homeZ = (int) player.getZ();
        data.hasHomeSet = true;
        saveAll();
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a庇護所重生點已設定！"));
    }

    /**
     * 傳送回庇護所。
     */
    public static void teleportToHome(ServerPlayer player) {
        BaseData data = playerBases.get(player.getUUID());
        if (data == null || !data.hasHomeSet) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c尚未設定庇護所重生點，請使用 /base sethome"));
            return;
        }
        // 冷卻檢查（60 秒）
        if (homeCooldowns.containsKey(player.getUUID()) &&
                System.currentTimeMillis() - homeCooldowns.get(player.getUUID()) < 60000) {
            long remaining = 60 - (System.currentTimeMillis() - homeCooldowns.get(player.getUUID())) / 1000;
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c傳送冷卻中，剩餘 " + remaining + " 秒"));
            return;
        }
        var server = player.getServer();
        if (server == null) return;
        var level = server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new net.minecraft.resources.ResourceLocation(data.homeWorld)));
        if (level == null) return;
        player.teleportTo(level, data.homeX + 0.5, data.homeY + 1.0, data.homeZ + 0.5,
                player.getYRot(), player.getXRot());
        homeCooldowns.put(player.getUUID(), System.currentTimeMillis());
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a已傳送回庇護所"));
    }

    /** 升級花費（哈夫幣） */
    private static final long[][] UPGRADE_COSTS = {
        // storageLevel, workbenchLevel, medicalLevel
        {0, 0, 0},          // Lv1
        {5000, 3000, 4000}, // Lv2
        {15000, 10000, 12000}, // Lv3
        {50000, 0, 0},      // Lv4 (僅儲物)
        {150000, 0, 0}      // Lv5 (僅儲物)
    };

    /**
     * 升級庇護所設施。
     * @param type "storage" / "workbench" / "medical"
     */
    public static String upgrade(ServerPlayer player, String type) {
        BaseData data = getOrCreate(player);
        int currentLevel;
        long cost;

        switch (type) {
            case "storage" -> {
                currentLevel = data.storageLevel;
                if (currentLevel >= 5) return "§c儲物等級已達最高！";
                cost = UPGRADE_COSTS[currentLevel][0];
                data.storageLevel++;
            }
            case "workbench" -> {
                currentLevel = data.workbenchLevel;
                if (currentLevel >= 3) return "§c工作台等級已達最高！";
                cost = UPGRADE_COSTS[currentLevel][1];
                data.workbenchLevel++;
            }
            case "medical" -> {
                currentLevel = data.medicalLevel;
                if (currentLevel >= 3) return "§c醫療等級已達最高！";
                cost = UPGRADE_COSTS[currentLevel][2];
                data.medicalLevel++;
            }
            default -> {
                return "§c未知的升級類型：" + type;
            }
        }

        // 扣款
        long balance = com.deltaops.lobby.EconomyManager.getBalance(player);
        if (balance < cost) {
            return "§c哈夫幣不足！需要 $" + cost + "，你只有 $" + balance;
        }
        com.deltaops.lobby.EconomyManager.addBalance(player, -cost);
        saveAll();

        return "§a升級成功！" + type + " 等級 " + (currentLevel) + " → " + (currentLevel + 1)
                + " (花費 $" + cost + ")";
    }

    /**
     * 查詢庇護所資訊。
     */
    public static String getInfo(ServerPlayer player) {
        BaseData data = getOrCreate(player);
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== 庇護所資訊 ===\n");
        sb.append("§e儲物等級: §f").append(data.storageLevel).append("/5\n");
        sb.append("§e工作台等級: §f").append(data.workbenchLevel).append("/3\n");
        sb.append("§e醫療等級: §f").append(data.medicalLevel).append("/3\n");
        sb.append("§e重生點: ").append(data.hasHomeSet ? "§a已設定" : "§7未設定").append("\n");
        return sb.toString();
    }

    // ========== 持久化 ==========

    private static void loadAll() {
        try {
            if (Files.exists(STORAGE_PATH)) {
                String json = Files.readString(STORAGE_PATH, StandardCharsets.UTF_8);
                if (!json.isBlank()) {
                    Map<UUID, BaseData> loaded = GSON.fromJson(json, DATA_TYPE);
                    if (loaded != null) {
                        playerBases.putAll(loaded);
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private static void saveAll() {
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
            Files.writeString(STORAGE_PATH, GSON.toJson(playerBases), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    // ========== 冷卻 ==========

    private static final Map<UUID, Long> homeCooldowns = new ConcurrentHashMap<>();

    // ========== 事件監聽 ==========

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            // 確保玩家有庇護所數據
            getOrCreate(player);
        }
    }
}
