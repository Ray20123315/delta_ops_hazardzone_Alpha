/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.lobby;

import com.deltaops.DeltaOpsMod;
import com.deltaops.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EconomyManager {
    private static final Path STORAGE_PATH = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve(DeltaOpsMod.MOD_ID).resolve("item_prices.json");
    private static final String PLAYER_BALANCE_TAG = DeltaOpsMod.MOD_ID + ":player_balance";
    private static final String DEFAULT_RESOURCE = "/default_item_prices.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type PRICE_MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();

    private static final Map<String, Long> ITEM_PRICES = new HashMap<>();

    static {
        init();
    }

    public static void init() {
        loadPrices();
    }

    public static void reloadPrices() {
        loadPrices();
    }

    public static void setItemPrice(String itemId, long price) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        ITEM_PRICES.put(itemId, Math.max(0L, price));
        savePrices();
    }

    private static void loadPrices() {
        try {
            if (!Files.exists(STORAGE_PATH)) {
                copyDefaultPrices();
            }
            if (Files.exists(STORAGE_PATH)) {
                String json = Files.readString(STORAGE_PATH, StandardCharsets.UTF_8);
                if (!json.isBlank()) {
                    // 第二層防線攔截：HMAC 簽名驗證
                    if (!com.deltaops.security.HMACConfigManager.verifyConfig(STORAGE_PATH)) {
                        DeltaOpsMod.LOGGER.error("⛔╔═══════════════════════════════════════════════╗");
                        DeltaOpsMod.LOGGER.error("⛔║  item_prices.json 簽名驗證失敗！          ║");
                        DeltaOpsMod.LOGGER.error("⛔║  檔案可能已被非法篡改！                    ║");
                        DeltaOpsMod.LOGGER.error("⛔║  正在還原為系統安全預設值...               ║");
                        DeltaOpsMod.LOGGER.error("⛔╚═══════════════════════════════════════════════╝");
                        ITEM_PRICES.clear();
                        // 嘗試從內建預設值還原
                        loadDefaultPrices();
                        return;
                    }
                    Map<String, Long> loaded = GSON.fromJson(json, PRICE_MAP_TYPE);
                    if (loaded != null) {
                        ITEM_PRICES.clear();
                        ITEM_PRICES.putAll(loaded);
                    }
                }
            }
        } catch (IOException e) {
            DeltaOpsMod.LOGGER.error("Failed to load item prices", e);
        }
    }

    /**
     * 從內建資源載入預設物價表（安全還原）。
     */
    private static void loadDefaultPrices() {
        try (InputStream stream = EconomyManager.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) {
                DeltaOpsMod.LOGGER.error("⛔ [Delta Ops] 無法載入內建預設物價表！物價系統為空！");
                return;
            }
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (!json.isBlank()) {
                Map<String, Long> loaded = GSON.fromJson(json, PRICE_MAP_TYPE);
                if (loaded != null) {
                    ITEM_PRICES.putAll(loaded);
                    DeltaOpsMod.LOGGER.warn("⚠️ [Delta Ops] 已從內建預設值還原物價表 ({} 項)", loaded.size());
                }
            }
        } catch (IOException e) {
            DeltaOpsMod.LOGGER.error("⛔ [Delta Ops] 載入內建預設物價表失敗", e);
        }
    }

    private static void copyDefaultPrices() {
        try (InputStream stream = EconomyManager.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) {
                return;
            }
            Files.createDirectories(STORAGE_PATH.getParent());
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(STORAGE_PATH, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            DeltaOpsMod.LOGGER.error("Failed to create default item_prices.json", e);
        }
    }

    private static void savePrices() {
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
            Files.writeString(STORAGE_PATH, GSON.toJson(ITEM_PRICES), StandardCharsets.UTF_8);
            // 寫入後自動簽名
            com.deltaops.security.HMACConfigManager.signConfig(STORAGE_PATH);
        } catch (IOException e) {
            DeltaOpsMod.LOGGER.error("Failed to save item prices", e);
        }
    }

    public static long getBalance(ServerPlayer player) {
        if (player == null) {
            return 0L;
        }
        return player.getPersistentData().getLong(PLAYER_BALANCE_TAG);
    }

    public static void addBalance(ServerPlayer player, long amount) {
        // 第一層防線攔截：代碼完整性驗證失敗時全面鎖死經濟
        if (com.deltaops.security.CodeIntegrityValidator.getStatus()
                == com.deltaops.security.CodeIntegrityValidator.IntegrityStatus.INVALID) {
            DeltaOpsMod.LOGGER.error("⛔ [Delta Ops] 經濟系統已鎖死：核心代碼完整性驗證失敗！");
            DeltaOpsMod.LOGGER.error("⛔ [Delta Ops] 拒絕執行 addBalance(" + amount + ") 給予玩家 " + player.getName().getString());
            return;
        }

        if (player == null || amount == 0L) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        long current = data.getLong(PLAYER_BALANCE_TAG);
        data.putLong(PLAYER_BALANCE_TAG, Math.max(0L, current + amount));
    }

    public static long calculateExtractionValue(ServerPlayer player) {
        if (player == null) {
            return 0L;
        }
        long total = 0L;
        total += calculateItemCollectionValue(player.getInventory().items);
        total += calculateItemCollectionValue(player.getInventory().armor);
        total += calculateItemCollectionValue(player.getInventory().offhand);
        return total;
    }

    private static long calculateItemCollectionValue(Iterable<ItemStack> stacks) {
        long total = 0L;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            total += getItemPrice(stack) * stack.getCount();
        }
        return total;
    }

    public static long getItemPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        Item item = stack.getItem();
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
        if (registryName == null) {
            return 0L;
        }
        return ITEM_PRICES.getOrDefault(registryName.toString(), 0L);
    }

    /** 根據物品 ID 查詢價格 */
    public static long getItemPrice(String itemId) {
        if (itemId == null || itemId.isBlank()) return 0L;
        return ITEM_PRICES.getOrDefault(itemId, 0L);
    }

    public static int getItemCount() {
        return ITEM_PRICES.size();
    }

    public static long settleExtraction(ServerPlayer player, String mapName) {
        if (player == null) {
            return 0L;
        }
        long value = calculateExtractionValue(player);
        long bonus = 0L;
        if (mapName != null && !mapName.isBlank()) {
            MapDefinition mapDefinition = HazardMapRegistry.getMap(mapName);
            if (mapDefinition != null) {
                bonus = Math.max(0L, mapDefinition.minGearValue() / 10L);
            }
        }
        double mult = ModConfig.getRewardMultiplier();
        long reward = Math.max(0L, (long) ((value + bonus) * mult));
        if (reward > 0L) {
            addBalance(player, reward);
        }
        return reward;
    }
}
