/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.client;

import com.deltaops.DeltaOpsMod;
import com.deltaops.network.ModNetwork;
import com.deltaops.network.ServerboundOpenSecureBoxPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyBindings {
    // 翻譯鍵
    private static final String CATEGORY = "key.category.delta_ops_hazardzone.tactical";

    public static final KeyMapping OPEN_SECURE_BOX = new KeyMapping("key.delta_ops_hazardzone.open_secure_box", GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping OPEN_MENU      = new KeyMapping("key.delta_ops_hazardzone.open_menu",      GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping OPEN_STASH     = new KeyMapping("key.delta_ops_hazardzone.open_stash",     GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping OPEN_SHOP      = new KeyMapping("key.delta_ops_hazardzone.open_shop",      GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping OPEN_QUESTS    = new KeyMapping("key.delta_ops_hazardzone.open_quests",    GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping OPEN_SKILLS    = new KeyMapping("key.delta_ops_hazardzone.open_skills",    GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping OPEN_SELLGUI   = new KeyMapping("key.delta_ops_hazardzone.open_sellgui",   GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping OPEN_BALANCE   = new KeyMapping("key.delta_ops_hazardzone.open_balance",   GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    // OP 管理員按鍵
    public static final KeyMapping OPEN_ADMIN_CONFIG = new KeyMapping("key.delta_ops_hazardzone.open_admin_config", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping OPEN_ADMIN_ITEMS  = new KeyMapping("key.delta_ops_hazardzone.open_admin_items",  GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    public static final KeyMapping OPEN_DIAG         = new KeyMapping("key.delta_ops_hazardzone.open_diag",         GLFW.GLFW_KEY_UNKNOWN, CATEGORY);

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SECURE_BOX);
        event.register(OPEN_MENU);
        event.register(OPEN_STASH);
        event.register(OPEN_SHOP);
        event.register(OPEN_QUESTS);
        event.register(OPEN_SKILLS);
        event.register(OPEN_SELLGUI);
        event.register(OPEN_BALANCE);
        event.register(OPEN_ADMIN_CONFIG);
        event.register(OPEN_ADMIN_ITEMS);
        event.register(OPEN_DIAG);
    }

    @Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return;
            }

            // 安全箱（使用封包）
            if (OPEN_SECURE_BOX.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new ServerboundOpenSecureBoxPacket());
                return;
            }

            // 以下按鍵綁定透過執行聊天指令觸發（保留所有指令原有邏輯）
            if (OPEN_MENU.consumeClick()) {
                mc.player.connection.sendCommand("dt menu");
                return;
            }
            if (OPEN_STASH.consumeClick()) {
                mc.player.connection.sendCommand("dt stash");
                return;
            }
            if (OPEN_SHOP.consumeClick()) {
                mc.player.connection.sendCommand("dt shop");
                return;
            }
            if (OPEN_QUESTS.consumeClick()) {
                mc.player.connection.sendCommand("dt quests");
                return;
            }
            if (OPEN_SKILLS.consumeClick()) {
                mc.player.connection.sendCommand("dt skills");
                return;
            }
            if (OPEN_SELLGUI.consumeClick()) {
                mc.player.connection.sendCommand("dt sellgui");
                return;
            }
            if (OPEN_BALANCE.consumeClick()) {
                mc.player.connection.sendCommand("dt balance");
                return;
            }

            // OP 管理員按鍵（僅 OP 能執行）
            if (OPEN_ADMIN_CONFIG.consumeClick()) {
                mc.player.connection.sendCommand("dt admin config");
                return;
            }
            if (OPEN_ADMIN_ITEMS.consumeClick()) {
                mc.player.connection.sendCommand("dt admin items");
                return;
            }
            if (OPEN_DIAG.consumeClick()) {
                mc.player.connection.sendCommand("dt diag");
                return;
            }
        }
    }
}
