/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.hud;

import com.deltaops.DeltaOpsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 可拖拽自訂 HUD 位置管理器。
 * 玩家可以按住 ALT + 拖拽來移動 HUD 元素位置。
 * 位置儲存在客戶端設定檔中。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, value = Dist.CLIENT)
public class HudPositionManager {

    /** HUD 元素 ID 常量 */
    public static final String ELEM_TEAM_HUD = "team_hud";
    public static final String ELEM_HEALTH = "health";
    public static final String ELEM_KILL_FEED = "kill_feed";
    public static final String ELEM_HIT_MARKER = "hit_marker";
    public static final String ELEM_AMMO = "ammo";

    public static class HudElement {
        public String id;
        public int x, y;
        public boolean visible = true;

        public HudElement() {}
        public HudElement(String id, int x, int y) {
            this.id = id; this.x = x; this.y = y;
        }
    }

    private static final Path STORAGE = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve(DeltaOpsMod.MOD_ID).resolve("hud_positions.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Map<String, HudElement>>() {}.getType();

    private static final Map<String, HudElement> elements = new HashMap<>();

    // 拖拽狀態
    private static boolean isDragging = false;
    private static String draggingId = null;
    private static int dragOffsetX, dragOffsetY;

    static {
        // 預設位置
        elements.put(ELEM_TEAM_HUD, new HudElement(ELEM_TEAM_HUD, 8, 8));
        elements.put(ELEM_HEALTH, new HudElement(ELEM_HEALTH, 8, 30));
        elements.put(ELEM_KILL_FEED, new HudElement(ELEM_KILL_FEED, 0, 10));
        elements.put(ELEM_HIT_MARKER, new HudElement(ELEM_HIT_MARKER, 0, 0));
        elements.put(ELEM_AMMO, new HudElement(ELEM_AMMO, 0, 0));
        load();
    }

    public static HudElement getElement(String id) {
        return elements.computeIfAbsent(id, k -> new HudElement(id, 0, 0));
    }

    public static boolean isVisible(String id) {
        HudElement el = elements.get(id);
        return el != null && el.visible;
    }

    public static void setPosition(String id, int x, int y) {
        HudElement el = elements.computeIfAbsent(id, k -> new HudElement(id, x, y));
        el.x = x;
        el.y = y;
        save();
    }

    public static void toggleVisibility(String id) {
        HudElement el = elements.computeIfAbsent(id, k -> new HudElement(id, 0, 0));
        el.visible = !el.visible;
        save();
    }

    // ========== 拖拽處理 ==========

    /** 處理滑鼠點擊 — 開始拖拽（透過 ClientTick 監聽按鍵狀態） */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // 按住 SHIFT + 左鍵 才啟用拖拽模式
        boolean leftClick = mc.mouseHandler.isLeftPressed();
        boolean shiftHeld = mc.options.keyShift.isDown();

        if (leftClick && shiftHeld && !isDragging) {
            int mx = (int) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth());
            int my = (int) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());

            for (HudElement el : elements.values()) {
                if (!el.visible) continue;
                int w = 100, h = 16;
                if (mx >= el.x && mx <= el.x + w && my >= el.y && my <= el.y + h) {
                    isDragging = true;
                    draggingId = el.id;
                    dragOffsetX = mx - el.x;
                    dragOffsetY = my - el.y;
                    break;
                }
            }
        }

        // 釋放左鍵 → 停止拖拽並儲存
        if (!leftClick && isDragging) {
            isDragging = false;
            if (draggingId != null) {
                save();
                draggingId = null;
            }
        }

        // 拖拽中更新位置
        if (isDragging && draggingId != null) {
            double mx = mc.mouseHandler.xpos();
            double my = mc.mouseHandler.ypos();
            int guiX = (int) (mx * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth());
            int guiY = (int) (my * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
            HudElement el = elements.get(draggingId);
            if (el != null) {
                el.x = guiX - dragOffsetX;
                el.y = guiY - dragOffsetY;
            }
        }
    }

    /** 移除舊的 onMouseInput 方法 — 已整合到 onClientTick */

    // ========== 持久化 ==========

    private static void load() {
        try {
            if (Files.exists(STORAGE)) {
                String json = Files.readString(STORAGE, StandardCharsets.UTF_8);
                if (!json.isBlank()) {
                    Map<String, HudElement> loaded = GSON.fromJson(json, DATA_TYPE);
                    if (loaded != null) {
                        elements.putAll(loaded);
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private static void save() {
        try {
            Files.createDirectories(STORAGE.getParent());
            Files.writeString(STORAGE, GSON.toJson(elements), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
