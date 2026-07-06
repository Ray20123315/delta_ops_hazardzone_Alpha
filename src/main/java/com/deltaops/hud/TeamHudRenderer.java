/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.hud;

import com.deltaops.DeltaOpsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TeamHudRenderer {
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            return;
        }

        LocalPlayer localPlayer = mc.player;

        // 改用 LobbySquadManager 取得小隊成員（與觀戰系統一致）
        // 由於客戶端無法直接存取 LobbySquadManager，改為從 level.players() 中過濾
        // 實際上小隊 UUID 需要透過封包同步，這裡先用 TeamManager 降級方案
        com.deltaops.team.TeamManager.Team team = com.deltaops.team.TeamManager.getTeamByPlayer(localPlayer);
        if (team == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        int x = 8;
        int y = 8;

        for (ServerPlayer member : team.members) {
            if (member == null || member.getUUID().equals(localPlayer.getUUID())) {
                continue;
            }

            int health = (int) Math.ceil(member.getHealth());
            int maxHealth = (int) Math.ceil(member.getMaxHealth());

            // 顯示隊友名稱 + 血量數字 (HP: 75/100)
            gui.drawString(mc.font, Component.literal("§f" + member.getGameProfile().getName()), x, y, 0xFFFFFFFF, true);
            gui.drawString(mc.font, Component.literal("§c" + health + "§7/§c" + maxHealth), x + 80, y, 0xFF4444, true);
            y += 12;
        }
    }
}
