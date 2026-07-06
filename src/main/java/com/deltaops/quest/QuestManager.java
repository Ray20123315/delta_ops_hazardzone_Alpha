/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.quest;

import com.deltaops.DeltaOpsMod;
import com.deltaops.lobby.EconomyManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任務管理器：處理任務進度追蹤與獎勵結算。
 * 任務資料儲存於玩家 PersistentData。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class QuestManager {
    private static final String TAG_QUESTS = DeltaOpsMod.MOD_ID + ":active_quests";
    private static final String TAG_PROGRESS = DeltaOpsMod.MOD_ID + ":quest_progress";
    private static final String TAG_COMPLETED = DeltaOpsMod.MOD_ID + ":completed_quests";

    private static final int DAILY_QUEST_COUNT = 3;

    /**
     * 為玩家分配每日任務。
     */
    public static void assignDailyQuests(ServerPlayer player) {
        if (player == null) return;
        CompoundTag data = player.getPersistentData();
        List<String> completedIds = getCompletedQuestIds(player);
        List<QuestData.QuestTemplate> selected = QuestData.pickRandomQuests(DAILY_QUEST_COUNT, completedIds);

        ListTag questList = new ListTag();
        ListTag progressList = new ListTag();
        for (QuestData.QuestTemplate qt : selected) {
            questList.add(StringTag.valueOf(qt.id()));
            progressList.add(net.minecraft.nbt.IntTag.valueOf(0));
        }
        data.put(TAG_QUESTS, questList);
        data.put(TAG_PROGRESS, progressList);
    }

    /**
     * 取得玩家當前活躍任務與進度。
     */
    public static List<ActiveQuest> getActiveQuests(ServerPlayer player) {
        List<ActiveQuest> result = new ArrayList<>();
        if (player == null) return result;
        CompoundTag data = player.getPersistentData();
        ListTag questList = data.getList(TAG_QUESTS, net.minecraft.nbt.Tag.TAG_STRING);
        ListTag progressList = data.getList(TAG_PROGRESS, net.minecraft.nbt.Tag.TAG_INT);

        for (int i = 0; i < questList.size(); i++) {
            String id = questList.getString(i);
            int progress = i < progressList.size() ? progressList.getInt(i) : 0;
            Optional<QuestData.QuestTemplate> opt = QuestData.getAllQuests().stream().filter(q -> q.id().equals(id)).findFirst();
            opt.ifPresent(qt -> result.add(new ActiveQuest(qt, progress)));
        }
        return result;
    }

    /**
     * 取得玩家已完成的所有任務 ID。
     */
    public static List<String> getCompletedQuestIds(ServerPlayer player) {
        List<String> result = new ArrayList<>();
        if (player == null) return result;
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList(TAG_COMPLETED, net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    /**
     * 增加指定類型的任務進度（如擊殺怪物）。
     */
    public static void addProgress(ServerPlayer player, QuestData.QuestType type, String targetId, int amount) {
        if (player == null) return;
        CompoundTag data = player.getPersistentData();
        ListTag questList = data.getList(TAG_QUESTS, net.minecraft.nbt.Tag.TAG_STRING);
        ListTag progressList = data.getList(TAG_PROGRESS, net.minecraft.nbt.Tag.TAG_INT);

        boolean changed = false;
        for (int i = 0; i < questList.size(); i++) {
            String qid = questList.getString(i);
            Optional<QuestData.QuestTemplate> opt = QuestData.getAllQuests().stream().filter(q -> q.id().equals(qid)).findFirst();
            if (opt.isEmpty()) continue;
            QuestData.QuestTemplate qt = opt.get();
            if (qt.type() != type) continue;
            if (!qt.targetId().isEmpty() && !qt.targetId().equals(targetId)) continue;

            int current = i < progressList.size() ? progressList.getInt(i) : 0;
            int newProgress = Math.min(qt.requiredCount(), current + amount);
            if (i < progressList.size()) {
                progressList.set(i, net.minecraft.nbt.IntTag.valueOf(newProgress));
            } else {
                progressList.add(net.minecraft.nbt.IntTag.valueOf(newProgress));
            }
            changed = true;

            // 任務完成
            if (newProgress >= qt.requiredCount()) {
                completeQuest(player, qt);
            }
        }

        if (changed) {
            data.put(TAG_PROGRESS, progressList);
        }
    }

    private static void completeQuest(ServerPlayer player, QuestData.QuestTemplate qt) {
        // 移除活躍任務
        CompoundTag data = player.getPersistentData();
        ListTag questList = data.getList(TAG_QUESTS, net.minecraft.nbt.Tag.TAG_STRING);
        ListTag progressList = data.getList(TAG_PROGRESS, net.minecraft.nbt.Tag.TAG_INT);

        // 標記為已完成
        ListTag completedList = data.getList(TAG_COMPLETED, net.minecraft.nbt.Tag.TAG_STRING);
        completedList.add(StringTag.valueOf(qt.id()));
        data.put(TAG_COMPLETED, completedList);

        // 發獎勵
        EconomyManager.addBalance(player, qt.reward());
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a§l✅ 任務完成！[" + qt.title() + "] 獲得 " + qt.reward() + " 哈夫幣！"));

        // 從活躍列表移除（可選：只隱藏保留，下次重新指派）
        int idx = -1;
        for (int i = 0; i < questList.size(); i++) {
            if (questList.getString(i).equals(qt.id())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            questList.remove(idx);
            if (idx < progressList.size()) progressList.remove(idx);
            data.put(TAG_QUESTS, questList);
            data.put(TAG_PROGRESS, progressList);
        }
    }

    /**
     * 事件：擊殺怪物時增加任務進度。
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        LivingEntity dead = event.getEntity();
        EntityType<?> type = dead.getType();
        String typeId = EntityType.getKey(type).toString();

        addProgress(killer, QuestData.QuestType.KILL, typeId, 1);
    }

    public record ActiveQuest(QuestData.QuestTemplate template, int currentProgress) {
        public boolean isComplete() {
            return currentProgress >= template.requiredCount();
        }
    }
}
