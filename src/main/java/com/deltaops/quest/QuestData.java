/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.quest;

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
        QUESTS.add(new QuestTemplate("kill_zombie_10", "獵殺殭屍", "擊殺 10 隻殭屍", QuestType.KILL, "minecraft:zombie", 10, 2000));
        QUESTS.add(new QuestTemplate("kill_skeleton_10", "獵殺骷髏", "擊殺 10 隻骷髏", QuestType.KILL, "minecraft:skeleton", 10, 2500));
        QUESTS.add(new QuestTemplate("kill_spider_5", "獵殺蜘蛛", "擊殺 5 隻蜘蛛", QuestType.KILL, "minecraft:spider", 5, 1500));
        QUESTS.add(new QuestTemplate("kill_creeper_5", "獵殺苦力怕", "擊殺 5 隻苦力怕", QuestType.KILL, "minecraft:creeper", 5, 3000));
        QUESTS.add(new QuestTemplate("kill_player_3", "擊敗敵人", "擊殺 3 名玩家", QuestType.KILL, "minecraft:player", 3, 10000));
        QUESTS.add(new QuestTemplate("kill_enderman_3", "獵殺終界使者", "擊殺 3 隻終界使者", QuestType.KILL, "minecraft:enderman", 3, 5000));
        QUESTS.add(new QuestTemplate("kill_witch_2", "獵殺女巫", "擊殺 2 隻女巫", QuestType.KILL, "minecraft:witch", 2, 4000));
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
