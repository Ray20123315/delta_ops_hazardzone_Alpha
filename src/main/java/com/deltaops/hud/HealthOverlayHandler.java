/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.hud;

import com.deltaops.DeltaOpsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 原版愛心渲染（50 顆心 = 100 HP）：
 * 始終只顯示一排 10 顆心，不同血量層級用顏色區分。
 * 顏色由高到低：紅 → 橙 → 金 → 綠 → 灰
 *
 *   100 HP → 10 顆紅心
 *   90 HP → 5 顆紅心 + 5 顆橙心
 *   81 HP → 半顆紅心 + 9.5 顆橙心
 *   61 HP → 半顆橙心 + 9.5 顆金心
 *   1 HP  → 半顆灰心
 *
 * 使用 RenderSystem.setShaderColor 著色，保留原版愛心形狀。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, value = Dist.CLIENT)
public class HealthOverlayHandler {

    private static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");

    // 5 層顏色（由高到低）：紅橙金綠灰
    private static final int[] LAYER_COLORS = {
            0xFFFF4444, // 第 5 層：紅   (81~100)
            0xFFFF8800, // 第 4 層：橙   (61~80)
            0xFFFFD700, // 第 3 層：金   (41~60)
            0xFF44CC44, // 第 2 層：綠   (21~40)
            0xFF888888  // 第 1 層：灰   (1~20)
    };

    // 層級縮寫標籤
    private static final String[] LAYER_TAGS = {
            "§cR", "§6O", "§eG", "§aG", "§7A"
    };

    private static final int HEART_SPACING = 11;
    private static final int HEART_SIZE = 9;
    private static final int HEARTS_PER_ROW = 10;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        float maxHealth = player.getMaxHealth();

        if (maxHealth <= 20.0f) return;

        float currentHealth = player.getHealth();
        GuiGraphics gui = event.getGuiGraphics();

        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // 計算當前血量所在的層 (0-based) 與該層內進度
        int currentLayer = Math.min(4, Math.max(0, (int) Math.ceil(currentHealth / 20.0) - 1));
        if (currentHealth <= 0) currentLayer = 0;

        // 該層已消耗的 HP（從該層底部往上算）
        int layerBottom = currentLayer * 20;
        float consumed = currentHealth - layerBottom;
        if (consumed < 0) consumed = 0;
        if (consumed > 20) consumed = 20;

        int fullHeartsCurrent = (int) Math.floor(consumed / 2.0);
        boolean hasHalfCurrent = (consumed % 2.0) >= 1.0;

        // 計算位置
        int totalWidth = HEARTS_PER_ROW * HEART_SPACING;
        int startX = (screenWidth - totalWidth) / 2;
        int startY = mc.getWindow().getGuiScaledHeight() - 39;

        // 標籤
        String tag = LAYER_TAGS[currentLayer];
        gui.drawString(mc.font, tag + " " + (int) Math.floor(currentHealth) + "/100", startX + totalWidth + 5, startY + 1, 0xFFFFFF, true);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int heartIndex = 0; heartIndex < 10; heartIndex++) {
            int x = startX + heartIndex * HEART_SPACING;
            int y = startY;

            // 決定這顆心的顏色
            int heartColor;
            boolean isHalf = false;

            if (heartIndex < fullHeartsCurrent) {
                // 已消耗 → 用下一層（更低層）的顏色顯示為滿
                heartColor = getLayerColor(currentLayer - 1);
            } else if (heartIndex == fullHeartsCurrent && hasHalfCurrent) {
                // 半心：用當前層顏色，顯示半心
                heartColor = LAYER_COLORS[currentLayer];
                isHalf = true;
            } else {
                // 未消耗：用當前層顏色，顯示滿心
                heartColor = LAYER_COLORS[currentLayer];
            }

            // 1. 先畫背景空愛心（不著色）
            gui.blit(GUI_ICONS, x, y, 16, 0, 9, 9, 9, 9, 256, 256);

            // 2. 用著色方式畫填充愛心
            float r = ((heartColor >> 16) & 0xFF) / 255.0f;
            float g = ((heartColor >> 8) & 0xFF) / 255.0f;
            float b = (heartColor & 0xFF) / 255.0f;
            RenderSystem.setShaderColor(r, g, b, 1.0f);

            if (isHalf) {
                gui.blit(GUI_ICONS, x, y, 61, 0, 9, 9, 9, 9, 256, 256);
            } else {
                gui.blit(GUI_ICONS, x, y, 52, 0, 9, 9, 9, 9, 256, 256);
            }
        }

        // 重置顏色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static int getLayerColor(int layer) {
        if (layer < 0) layer = 0;
        if (layer > 4) layer = 4;
        return LAYER_COLORS[layer];
    }
}
