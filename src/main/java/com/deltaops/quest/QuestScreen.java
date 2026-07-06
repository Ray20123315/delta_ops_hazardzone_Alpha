/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.quest;

import com.deltaops.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 任務 GUI 面板 — 顯示每日任務列表與進度。
 */
public class QuestScreen extends Screen {
    private List<QuestManager.ActiveQuest> activeQuests;
    private String statusMessage = "";

    public QuestScreen() {
        super(Component.literal("每日任務"));
    }

    @Override
    protected void init() {
        super.init();

        // 請求伺服端發送任務資料
        ModNetwork.CHANNEL.sendToServer(new ServerboundQuestActionPacket(ServerboundQuestActionPacket.Action.REFRESH));

        int cx = width / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("🔄 刷新任務"),
                btn -> {
                    ModNetwork.CHANNEL.sendToServer(new ServerboundQuestActionPacket(ServerboundQuestActionPacket.Action.REFRESH));
                    statusMessage = "§e正在刷新任務...";
                }
        ).bounds(cx - 60, height - 60, 120, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("關閉"),
                btn -> this.onClose()
        ).bounds(cx - 40, height - 35, 80, 20).build());
    }

    /**
     * 由網路封包處理器呼叫，更新任務資料。
     */
    public void updateQuests(List<QuestManager.ActiveQuest> quests) {
        this.activeQuests = quests;
        if (quests.isEmpty()) {
            statusMessage = "§e暫無活躍任務，請按「刷新任務」獲取。";
        } else {
            statusMessage = "";
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);

        int cx = width / 2;
        int top = 30;

        // 標題
        gui.drawString(font, Component.literal("§6§l📋 每日任務"), cx - 50, top, 0xFFFFFF);

        // 狀態訊息
        if (!statusMessage.isEmpty()) {
            gui.drawString(font, Component.literal(statusMessage), cx - 100, top + 18, 0xCCCCCC);
        }

        // 任務列表
        if (activeQuests != null && !activeQuests.isEmpty()) {
            int y = top + 40;
            for (int i = 0; i < activeQuests.size(); i++) {
                QuestManager.ActiveQuest aq = activeQuests.get(i);
                QuestData.QuestTemplate t = aq.template();

                // 任務標題 + 進度條背景
                gui.fill(cx - 140, y, cx + 140, y + 50, 0x44222A37);
                gui.fill(cx - 140, y, cx + 140, y + 50, 0x88303A4A);

                // 任務名稱
                gui.drawString(font, Component.literal("§e" + (i + 1) + ". " + t.title()), cx - 130, y + 4, 0xFFFFAA);
                // 任務描述
                gui.drawString(font, Component.literal("§7" + t.description()), cx - 130, y + 16, 0xAAAAAA);

                // 進度條
                int barWidth = 200;
                int barX = cx - 100;
                int barY = y + 30;
                int progress = Math.min(100, (int) ((double) aq.currentProgress() / t.requiredCount() * 100));

                gui.fill(barX, barY, barX + barWidth, barY + 8, 0xFF333333);
                int barColor = aq.isComplete() ? 0xFF55FF55 : 0xFFFFAA00;
                gui.fill(barX, barY, barX + (int) (barWidth * progress / 100.0), barY + 8, barColor);

                String progressText = aq.isComplete()
                        ? "§a✅ " + t.reward() + " 哈夫幣"
                        : "§e" + aq.currentProgress() + "/" + t.requiredCount() + "  §7獎勵: " + t.reward() + " 哈夫幣";
                gui.drawString(font, Component.literal(progressText), barX + 5, barY - 1, 0xFFFFFF);

                y += 56;
            }
        } else if (activeQuests != null && activeQuests.isEmpty()) {
            gui.drawString(font, Component.literal("§7暫無活躍任務"), cx - 50, top + 50, 0x888888);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
