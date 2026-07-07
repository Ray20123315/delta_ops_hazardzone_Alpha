/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能系統管理器：
 * 玩家透過完成任務獲得技能點，消耗技能點學習被動技能。
 * 技能資料儲存在玩家 PersistentData NBT 中。
 */
@Mod.EventBusSubscriber(modid = com.deltaops.DeltaOpsMod.MOD_ID)
public class SkillManager {

    private static final String SKILL_TAG = "DeltaOpsSkills";
    private static final String POINTS_TAG = "SkillPoints";

    // 技能定義：ID -> 名稱、最大等級、每級效果描述
    public static final Map<String, SkillDef> SKILLS = new HashMap<>();

    static {
        SKILLS.put("toughness", new SkillDef("強健體魄", 5,
                "最大生命 +10", "最大生命 +20", "最大生命 +30", "最大生命 +40", "最大生命 +50"));
        SKILLS.put("runner", new SkillDef("跑者", 3,
                "移動速度 +5%", "移動速度 +10%", "移動速度 +15%"));
        SKILLS.put("strength", new SkillDef("力量", 3,
                "近戰傷害 +10%", "近戰傷害 +20%", "近戰傷害 +30%"));
        SKILLS.put("gatherer", new SkillDef("採集者", 3,
                "額外戰利品機率 +10%", "額外戰利品機率 +20%", "額外戰利品機率 +30%"));
    }

    /** 取得玩家技能點數 */
    public static int getSkillPoints(ServerPlayer player) {
        CompoundTag data = player.getPersistentData().getCompound(SKILL_TAG);
        return data.getInt(POINTS_TAG);
    }

    /** 增加技能點數 */
    public static void addSkillPoints(ServerPlayer player, int amount) {
        CompoundTag data = player.getPersistentData().getCompound(SKILL_TAG);
        int current = data.getInt(POINTS_TAG);
        data.putInt(POINTS_TAG, current + amount);
        player.getPersistentData().put(SKILL_TAG, data);
    }

    /** 取得特定技能的等級 (0 = 未學習) */
    public static int getSkillLevel(ServerPlayer player, String skillId) {
        CompoundTag data = player.getPersistentData().getCompound(SKILL_TAG);
        return data.getInt("level_" + skillId);
    }

    /** 嘗試升級技能（消耗 1 點技能點） */
    public static boolean upgradeSkill(ServerPlayer player, String skillId) {
        SkillDef def = SKILLS.get(skillId);
        if (def == null) return false;

        int currentLevel = getSkillLevel(player, skillId);
        if (currentLevel >= def.maxLevel) return false;

        int points = getSkillPoints(player);
        if (points < 1) return false;

        // 扣點並升級
        CompoundTag data = player.getPersistentData().getCompound(SKILL_TAG);
        data.putInt(POINTS_TAG, points - 1);
        data.putInt("level_" + skillId, currentLevel + 1);
        player.getPersistentData().put(SKILL_TAG, data);

        // 套用技能效果
        applySkillEffects(player, skillId, currentLevel + 1);
        return true;
    }

    /** 套用技能效果 */
    private static void applySkillEffects(ServerPlayer player, String skillId, int level) {
        switch (skillId) {
            case "toughness" -> {
                AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null) {
                    attr.setBaseValue(100.0 + level * 10);
                    player.setHealth((float) Math.min(player.getHealth() + 10, attr.getValue()));
                }
            }
            case "runner" -> {
                AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attr != null) {
                    attr.setBaseValue(0.1 * (1.0 + level * 0.05));
                }
            }
            // strength 和 gatherer 在戰鬥/戰利品系統中檢查
        }
    }

    /** 登入時套用所有已學技能 */
    public static void applyAllSkills(ServerPlayer player) {
        CompoundTag data = player.getPersistentData().getCompound(SKILL_TAG);
        for (String skillId : SKILLS.keySet()) {
            int level = data.getInt("level_" + skillId);
            if (level > 0) {
                applySkillEffects(player, skillId, level);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyAllSkills(player);
    }

    /** 技能定義 */
    public record SkillDef(String name, int maxLevel, String... descriptions) {}
}
