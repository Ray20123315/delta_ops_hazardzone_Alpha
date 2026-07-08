/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.client.squad;

import com.deltaops.lobby.HazardMapRegistry;
import com.deltaops.lobby.MapDefinition;
import com.deltaops.network.ModNetwork;
import com.deltaops.network.squad.ClientboundSquadStatusPacket;
import com.deltaops.network.squad.ServerboundSquadActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 整合小隊面板 + 配對大廳。
 * - 上方：小隊管理（建立/邀請/踢出/移交/離開）
 * - 中間：地圖選擇 + 準備/開始遊戲
 * - 下方：配對佇列狀態 + 排隊控制
 * - 關閉畫面（ESC）時不影響配對狀態
 */
public class SquadMainScreen extends Screen {
    private EditBox targetNameInput;
    private UUID leaderUuid;
    private final List<String> memberNames = new ArrayList<>();
    private final List<Boolean> readyStates = new ArrayList<>();
    private String statusText = "等待小隊資料...";
    private Button mapSelectButton;
    private final List<MapDefinition> maps = new ArrayList<>(HazardMapRegistry.getAllMaps().values());
    private int selectedMapIndex = -1;

    // 配對狀態
    private boolean inQueue = false;
    private int queuedPlayers = 0;
    private Button queueButton;
    private Button readyButton;
    private Button launchButton;
    private int tickCounter = 0;
    private final String[] spinner = { "|", "/", "—", "\\" };

    public SquadMainScreen() {
        super(Component.literal("小隊面板"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int top = height / 5;

        // === 玩家名稱輸入 ===
        targetNameInput = new EditBox(font, centerX - 110, top + 24, 220, 20, Component.literal("玩家名稱"));
        targetNameInput.setValue("");
        targetNameInput.setHint(Component.literal("輸入玩家名稱"));
        targetNameInput.setCanLoseFocus(true);
        addRenderableWidget(targetNameInput);
        setInitialFocus(targetNameInput);

        // === 小隊管理按鈕列 ===
        addRenderableWidget(Button.builder(Component.literal("建立小隊"), btn -> sendAction(ServerboundSquadActionPacket.Action.CREATE, null)).bounds(centerX - 210, top + 60, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("離開小隊"), btn -> sendAction(ServerboundSquadActionPacket.Action.LEAVE, null)).bounds(centerX - 105, top + 60, 100, 20).build());
        this.readyButton = addRenderableWidget(Button.builder(Component.literal("切換準備"), btn -> sendAction(ServerboundSquadActionPacket.Action.TOGGLE_READY, null)).bounds(centerX + 5, top + 60, 100, 20).build());
        this.launchButton = addRenderableWidget(Button.builder(Component.literal("開始遊戲"), btn -> sendAction(ServerboundSquadActionPacket.Action.LAUNCH, null)).bounds(centerX + 110, top + 60, 100, 20).build());

        // === 邀請/踢出/移交 ===
        addRenderableWidget(Button.builder(Component.literal("邀請玩家"), btn -> sendAction(ServerboundSquadActionPacket.Action.INVITE, targetNameInput.getValue())).bounds(centerX - 210, top + 90, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("踢出玩家"), btn -> sendAction(ServerboundSquadActionPacket.Action.KICK, targetNameInput.getValue())).bounds(centerX - 105, top + 90, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("移交隊長"), btn -> sendAction(ServerboundSquadActionPacket.Action.TRANSFER, targetNameInput.getValue())).bounds(centerX + 5, top + 90, 100, 20).build());

        // === 地圖選擇 ===
        this.mapSelectButton = this.addRenderableWidget(Button.builder(
                Component.literal(getMapButtonLabel()),
                btn -> cycleMap()
        ).bounds(centerX - 105, top + 118, 210, 20).build());

        // === 配對控制（整合在下方，取代獨立配對大廳） ===
        int queueY = top + 148;
        this.queueButton = this.addRenderableWidget(Button.builder(
                Component.literal(inQueue ? "§e取消配對" : "§a開始配對"),
                btn -> toggleQueue()
        ).bounds(centerX - 105, queueY, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("🏪 商店"), btn -> ModNetwork.CHANNEL.sendToServer(new ServerboundSquadActionPacket(ServerboundSquadActionPacket.Action.OPEN_SHOP, null))).bounds(centerX, queueY, 100, 20).build());
    }

    /** 切換配對佇列狀態 */
    private void toggleQueue() {
        sendAction(ServerboundSquadActionPacket.Action.TOGGLE_AUTO_MATCH, null);
        inQueue = !inQueue;
        updateQueueButton();
    }

    private void updateQueueButton() {
        if (queueButton != null) {
            queueButton.setMessage(Component.literal(inQueue ? "§e取消配對" : "§a開始配對"));
        }
    }

    /** 從封包更新佇列狀態（由 ClientboundMatchStatusPacket 呼叫） */
    public void updateQueueStatus(int queuedPlayers, boolean matchOpen) {
        this.queuedPlayers = queuedPlayers;
        if (!matchOpen && inQueue) {
            inQueue = false;
            updateQueueButton();
        }
    }

    private String getMapButtonLabel() {
        if (selectedMapIndex < 0 || selectedMapIndex >= maps.size()) {
            return "§7請選擇地圖...";
        }
        MapDefinition md = maps.get(selectedMapIndex);
        return "§6地圖: " + md.displayName() + " (" + md.minPlayers() + "-" + md.maxPlayers() + "人)";
    }

    private void cycleMap() {
        selectedMapIndex = (selectedMapIndex + 1) % (maps.size() + 1) - 1;
        if (selectedMapIndex >= 0 && selectedMapIndex < maps.size()) {
            String mapId = maps.get(selectedMapIndex).mapId();
            sendAction(ServerboundSquadActionPacket.Action.SELECT_MAP, mapId);
        }
        if (mapSelectButton != null) {
            mapSelectButton.setMessage(Component.literal(getMapButtonLabel()));
        }
    }

    private void sendAction(ServerboundSquadActionPacket.Action action, String target) {
        ModNetwork.CHANNEL.sendToServer(new ServerboundSquadActionPacket(action, target));
    }

    public void updateSquadStatus(ClientboundSquadStatusPacket pkt) {
        this.leaderUuid = pkt.leaderUuid;
        this.memberNames.clear();
        this.readyStates.clear();
        this.memberNames.addAll(pkt.memberNames);
        this.readyStates.addAll(pkt.readyStates);
        this.statusText = "隊長: " + (pkt.leaderUuid != null ? pkt.leaderUuid.toString().substring(0, 8) : "未知") + "，成員: " + pkt.memberNames.size();
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (targetNameInput.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (targetNameInput.charTyped(chr, keyCode)) return true;
        return super.charTyped(chr, keyCode);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (targetNameInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);

        int centerX = width / 2;
        int top = height / 5;

        // 標題
        gui.drawString(font, Component.literal("§6§l小隊管理與配對大廳"), centerX - 100, top - 30, 0xFFFFFF);
        gui.drawString(font, Component.literal("輸入目標玩家名稱後，再執行邀請 / 踢出 / 移交。"), centerX - 180, top - 10, 0xAAAAAA);

        // 小隊資訊
        gui.drawString(font, Component.literal(this.statusText), centerX - 180, top + 138, 0xCCCCCC);

        // 配對狀態
        String queueStatus;
        if (inQueue) {
            String spin = spinner[(tickCounter / 10) % spinner.length];
            queueStatus = "§e配對中 " + spin + " §7| 佇列人數: " + queuedPlayers;
        } else {
            queueStatus = "§7目前不在配對佇列中 | 佇列人數: " + queuedPlayers;
        }
        gui.drawString(font, Component.literal(queueStatus), centerX - 180, top + 175, 0xFFFFFF);

        // 已選擇的地圖
        if (selectedMapIndex >= 0 && selectedMapIndex < maps.size()) {
            MapDefinition md = maps.get(selectedMapIndex);
            gui.drawString(font, Component.literal("§e已選: " + md.displayName() + " [" + md.minPlayers() + "-" + md.maxPlayers() + "人]"),
                    centerX - 180, top + 188, 0xFFFFAA);
        } else {
            gui.drawString(font, Component.literal("§7尚未選擇地圖"), centerX - 180, top + 188, 0x888888);
        }

        // 成員列表
        int listStartY = top + 200;
        for (int i = 0; i < memberNames.size(); i++) {
            String name = memberNames.get(i);
            boolean ready = i < readyStates.size() && readyStates.get(i);
            String label = name + (ready ? " (已準備)" : " (未準備)");
            gui.drawString(font, Component.literal(label), centerX - 180, listStartY + i * 12, ready ? 0x55FF55 : 0xFF5555);
        }
    }

    /**
     * 關閉畫面時不影響配對狀態。
     * 若玩家在佇列中，關閉 GUI 後仍維持配對。
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        // 不發送任何離開佇列的信號，維持當前配對狀態
        super.onClose();
    }
}
