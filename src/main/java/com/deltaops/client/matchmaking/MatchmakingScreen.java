/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.client.matchmaking;

import com.deltaops.network.ModNetwork;
import com.deltaops.network.squad.ServerboundSquadActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MatchmakingScreen extends Screen {
    private int queuedPlayers = 0;
    private boolean inQueue = false;
    private Button queueButton;
    private int tickCounter = 0;
    private final String[] spinner = { "|", "/", "—", "\\" };

    public MatchmakingScreen() {
        super(Component.literal("配對大廳"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;

        this.queueButton = this.addRenderableWidget(Button.builder(
                Component.literal(inQueue ? "取消配對" : "開始配對"),
                btn -> {
                    if (inQueue) {
                        // 取消配對 — 通知伺服器離開佇列
                        ModNetwork.CHANNEL.sendToServer(new ServerboundSquadActionPacket(
                                ServerboundSquadActionPacket.Action.TOGGLE_AUTO_MATCH, null));
                        inQueue = false;
                        queueButton.setMessage(Component.literal("開始配對"));
                    } else {
                        // 進入配對佇列
                        ModNetwork.CHANNEL.sendToServer(new ServerboundSquadActionPacket(
                                ServerboundSquadActionPacket.Action.TOGGLE_AUTO_MATCH, null));
                        inQueue = true;
                        queueButton.setMessage(Component.literal("取消配對"));
                    }
                }
        ).bounds(cx - 60, height / 2 + 40, 120, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("返回小隊面板"),
                btn -> minecraft.setScreen(new com.deltaops.client.squad.SquadMainScreen())
        ).bounds(cx - 60, height / 2 + 68, 120, 20).build());
    }

    /**
     * 供網路封包處理器呼叫，更新排隊狀態。
     */
    public void updateQueueStatus(int queuedPlayers, boolean matchOpen) {
        this.queuedPlayers = queuedPlayers;
        if (!matchOpen && inQueue) {
            inQueue = false;
            if (queueButton != null) {
                queueButton.setMessage(Component.literal("開始配對"));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);

        int cx = width / 2;
        int top = height / 3;

        gui.drawString(font, Component.literal("§6§l配對大廳"), cx - 40, top - 20, 0xFFFFFF);

        if (inQueue) {
            String spin = spinner[(tickCounter / 10) % spinner.length];
            gui.drawString(font, Component.literal("§e配對中 " + spin), cx - 40, top, 0xFFFFAA);
            gui.drawString(font, Component.literal("佇列人數: " + queuedPlayers), cx - 50, top + 16, 0xAAAAAA);
        } else {
            gui.drawString(font, Component.literal("§7目前不在配對佇列中"), cx - 60, top, 0xAAAAAA);
            gui.drawString(font, Component.literal("佇列人數: " + queuedPlayers), cx - 50, top + 16, 0xAAAAAA);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
