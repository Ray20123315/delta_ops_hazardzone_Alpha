/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.hud;

import com.deltaops.DeltaOpsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 數字化血量顯示（取代原版愛心條）：
 * 顯示為 ❤️ 目前HP / 最大HP，例如「❤️ 50/100」
 * 根據血量百分比變換愛心顏色。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HealthOverlayHandler {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            return;
        }

        // 取消原版渲染，由我們自己畫
        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        String text = "❤ " + (int) Math.ceil(currentHealth) + "/" + (int) Math.ceil(maxHealth);

        // 根據血量百分比決定愛心顏色
        float ratio = maxHealth > 0 ? currentHealth / maxHealth : 0;
        int color;
        if (ratio > 0.8f) {
            color = 0xFFFF4444;       // 紅（80%↑）
        } else if (ratio > 0.6f) {
            color = 0xFFFF8800;       // 橙（60%~80%）
        } else if (ratio > 0.4f) {
            color = 0xFFFFD700;       // 金（40%~60%）
        } else if (ratio > 0.2f) {
            color = 0xFF44CC44;       // 綠（20%~40%）
        } else {
            color = 0xFF888888;       // 灰（20%↓）
        }

        // 繪製在原版愛心條的位置（螢幕中央上方）
        int textX = (screenWidth - mc.font.width(text)) / 2;
        int textY = mc.getWindow().getGuiScaledHeight() - 40;

        gui.drawString(mc.font, text, textX, textY, color, true);
    }
}
