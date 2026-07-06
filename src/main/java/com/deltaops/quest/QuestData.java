/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.quest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 任務資料庫：定義所有可用任務的模板。
 */
public class QuestData {
    private static final List<QuestTemplate> QUESTS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        QUESTS.add(new QuestTemplate("collect_zombie_10", "獵殺殭屍", "擊殺 10 隻殭屍", QuestType.KILL, "minecraft:zombie", 10, 2000));
        QUESTS.add(new QuestTemplate("collect_skeleton_10", "獵殺骷髏", "擊殺 10 隻骷髏", QuestType.KILL, "minecraft:skeleton", 10, 2500));
        QUESTS.add(new QuestTemplate("collect_spider_5", "獵殺蜘蛛", "擊殺 5 隻蜘蛛", QuestType.KILL, "minecraft:spider", 5, 1500));
        QUESTS.add(new QuestTemplate("extract_1", "成功撤離", "成功撤離 1 次", QuestType.EXTRACTION, "", 1, 5000));
        QUESTS.add(new QuestTemplate("collect_diamond", "蒐集鑽石", "蒐集 3 顆鑽石", QuestType.COLLECT, "minecraft:diamond", 3, 8000));
        QUESTS.add(new QuestTemplate("collect_emerald", "蒐集綠寶石", "蒐集 5 顆綠寶石", QuestType.COLLECT, "minecraft:emerald", 5, 6000));
        QUESTS.add(new QuestTemplate("collect_iron", "蒐集鐵錠", "蒐集 16 個鐵錠", QuestType.COLLECT, "minecraft:iron_ingot", 16, 3000));
    }

    public static List<QuestTemplate> getAllQuests() {
        return Collections.unmodifiableList(QUESTS);
    }

    /**
     * 隨機選取指定數量的任務（排除玩家已完成的）。
     */
    public static List<QuestTemplate> pickRandomQuests(int count, List<String> excludeIds) {
        List<QuestTemplate> pool = new ArrayList<>(QUESTS);
        pool.removeIf(q -> excludeIds.contains(q.id()));
        Collections.shuffle(pool, RANDOM);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    public record QuestTemplate(String id, String title, String description, QuestType type, String targetId, int requiredCount, long reward) {}

    public enum QuestType {
        KILL,
        COLLECT,
        EXTRACTION
    }
}
