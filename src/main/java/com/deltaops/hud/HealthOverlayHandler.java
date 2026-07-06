/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.hud;

import com.deltaops.DeltaOpsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 自訂血量愛心渲染：
 * 總共 50 顆心 = 100 HP（每顆心 = 2 HP，半心 = 1 HP）。
 * 5 層，每層 10 顆心（= 20 HP）。
 *
 * 一次顯示一層（10 顆心），
 * 消耗上層的心後，空心的位置自動補上下一層的顏色。
 *
 * 例：
 *   100 HP → 10 顆紅心（第 5 層全滿）
 *   90 HP → 5 顆紅心（滿）+ 5 顆橙心（下一層）
 *   81 HP → 半顆紅心 + 9.5 顆橙心
 *   61 HP → 半顆橙心 + 9.5 顆金心
 *   1 HP  → 半顆灰心（第 1 層最後一顆半心）
 *
 * 層級顏色：
 *   第 5 層 (81~100 HP)：🔴 紅
 *   第 4 層 (61~80 HP) ：🟠 橙
 *   第 3 層 (41~60 HP) ：💛 金
 *   第 2 層 (21~40 HP) ：💚 綠
 *   第 1 層 (1~20 HP)  ：🩶 灰
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
        int layerBottom = currentLayer * 20;                  // 0, 20, 40, 60, 80
        float consumed = currentHealth - layerBottom;          // 0~20，該層用了多少
        if (consumed < 0) consumed = 0;
        if (consumed > 20) consumed = 20;

        // 已消耗的心數（半心為單位）
        // 每顆心 = 2 HP
        int fullHeartsCurrent = (int) Math.floor(consumed / 2.0);
        boolean hasHalfCurrent = (consumed % 2.0) >= 1.0;

        // 總心數 = 10，上層顏色填滿 consumed 的量，剩下的用下層顏色
        // 下層 = currentLayer - 1（但如果 currentLayer=0 就沒有下層，用灰色）

        // 計算位置
        int totalWidth = HEARTS_PER_ROW * HEART_SPACING;
        int startX = (screenWidth - totalWidth) / 2;
        int startY = mc.getWindow().getGuiScaledHeight() - 39;

        // 標籤
        String tag = LAYER_TAGS[currentLayer];
        gui.drawString(mc.font, tag + " " + (int) Math.floor(currentHealth) + "/100", startX + totalWidth + 5, startY + 1, 0xFFFFFF, true);

        for (int heartIndex = 0; heartIndex < 10; heartIndex++) {
            int x = startX + heartIndex * HEART_SPACING;
            int y = startY;

            // 決定這顆心的顏色和填充狀態
            int displayColor;
            boolean isFull = false;
            boolean isHalf = false;

            if (heartIndex < fullHeartsCurrent) {
                // 這顆心完全屬於上層（已消耗），用上層顏色但顯示為空（消耗掉了）
                // 但使用者說「消耗後補上下一層顏色」→ 所以消耗掉的用下一層顏色顯示為滿
                displayColor = getLayerColor(currentLayer - 1);
                isFull = true;
            } else if (heartIndex == fullHeartsCurrent && hasHalfCurrent) {
                // 正在消耗的這半顆：用上層顏色，半心
                displayColor = LAYER_COLORS[currentLayer];
                isHalf = true;
            } else {
                // 還沒消耗到：用上層顏色顯示為滿
                displayColor = LAYER_COLORS[currentLayer];
                isFull = true;
            }

            // 背景空愛心
            gui.blit(GUI_ICONS, x, y, 16, 0, 9, 9, 9, 9, 256, 256);

            if (isFull) {
                gui.blit(GUI_ICONS, x, y, 52, 0, 9, 9, 9, 9, 256, 256);
            } else if (isHalf) {
                gui.blit(GUI_ICONS, x, y, 61, 0, 9, 9, 9, 9, 256, 256);
            }

            // 疊加顏色
            gui.fill(x, y, x + HEART_SIZE, y + HEART_SIZE, (displayColor & 0x00FFFFFF) | 0xAA000000);
        }
    }

    private static int getLayerColor(int layer) {
        if (layer < 0) layer = 0;
        if (layer > 4) layer = 4;
        return LAYER_COLORS[layer];
    }
}
