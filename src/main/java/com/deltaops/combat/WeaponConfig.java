/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.combat;

import com.deltaops.DeltaOpsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 武器設定管理器：
 * 讀取 config/delta_ops_hazardzone/weapon_items.json
 * 讓玩家自行定義哪些物品視為「槍械」。
 */
public class WeaponConfig {

    private static final Path STORAGE = Paths.get("config", DeltaOpsMod.MOD_ID, "weapon_items.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    // 存放視為槍械的物品 registry name 列表
    private static final Set<String> WEAPON_IDS = new LinkedHashSet<>();

    static {
        load();
    }

    public static void load() {
        WEAPON_IDS.clear();
        try {
            if (Files.exists(STORAGE)) {
                String json = Files.readString(STORAGE, StandardCharsets.UTF_8);
                if (!json.isBlank()) {
                    List<String> loaded = GSON.fromJson(json, LIST_TYPE);
                    if (loaded != null) {
                        WEAPON_IDS.addAll(loaded);
                    }
                }
            }
        } catch (IOException ignored) {}

        // 若無資料，加入預設值
        if (WEAPON_IDS.isEmpty()) {
            WEAPON_IDS.add("tacz:modern_kinetic_gun"); // TaCZ 槍械
            WEAPON_IDS.add("minecraft:crossbow");
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(STORAGE.getParent());
            Files.writeString(STORAGE, GSON.toJson(new ArrayList<>(WEAPON_IDS)), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    /**
     * 檢查物品是否被設定為槍械。
     */
    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && WEAPON_IDS.contains(id.toString());
    }

    /**
     * 新增一個物品到槍械清單。
     */
    public static boolean addWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        boolean added = WEAPON_IDS.add(id.toString());
        if (added) save();
        return added;
    }

    /**
     * 從槍械清單移除一個物品。
     */
    public static boolean removeWeapon(String registryName) {
        if (registryName == null || registryName.isBlank()) return false;
        boolean removed = WEAPON_IDS.remove(registryName);
        if (removed) save();
        return removed;
    }

    /**
     * 取得所有已設定的槍械 registry 名稱。
     */
    public static Set<String> getAllWeaponIds() {
        return Collections.unmodifiableSet(WEAPON_IDS);
    }

    /**
     * 清空所有設定（還原為預設）。
     */
    public static void resetToDefault() {
        WEAPON_IDS.clear();
        WEAPON_IDS.add("tacz:modern_kinetic_gun");
        WEAPON_IDS.add("minecraft:crossbow");
        save();
    }
}
