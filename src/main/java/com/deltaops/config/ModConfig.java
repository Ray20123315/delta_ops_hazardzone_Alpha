package com.deltaops.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.LongValue DAM_REQUIRED_VALUE;
    public static final ForgeConfigSpec.ConfigValue<List<String>> ITEM_VALUE_MAPPING;

    // 動態設定（可透過管理員 GUI / 封包在遊戲中修改）
    private static String deathDropRule = "ALL";     // NONE, INVENTORY_ONLY, ALL
    private static int extractionTimerSeconds = 10;   // 撤離倒數秒數
    private static double rewardMultiplier = 1.0;     // 獎勵倍率

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.comment("Combat value and trader pricing configuration");

        DAM_REQUIRED_VALUE = BUILDER.comment("Minimum combat value required to enter the dam dimension")
                .defineInRange("dam_required_value", 112500L, 0L, Long.MAX_VALUE);

        ITEM_VALUE_MAPPING = BUILDER.comment("Item value mapping entries in format modid:itemid=value")
                .define("item_value_mapping", List.of(
                        "tacz:m4a1=35000",
                        "tacz:ak47=32000"
                ));

        SPEC = BUILDER.build();
    }

    public static long getItemValue(ResourceLocation registryName) {
        if (registryName == null) {
            return 1500L;
        }

        String target = registryName.toString();
        for (String entry : ITEM_VALUE_MAPPING.get()) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String trimmed = entry.trim();
            int splitIndex = trimmed.indexOf('=');
            if (splitIndex < 0) {
                continue;
            }

            String key = trimmed.substring(0, splitIndex).trim();
            String value = trimmed.substring(splitIndex + 1).trim();
            if (key.isBlank() || value.isBlank()) {
                continue;
            }

            if (key.equals(target)) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException ignored) {
                    return 1500L;
                }
            }
        }

        return 1500L;
    }

    // ========== 死亡掉落規則 ==========
    public static String getDeathDropRule() {
        return deathDropRule;
    }

    public static void setDeathDropRule(String rule) {
        if (rule == null || rule.isBlank()) return;
        String upper = rule.toUpperCase();
        if (upper.equals("NONE") || upper.equals("INVENTORY_ONLY") || upper.equals("ALL")) {
            deathDropRule = upper;
        }
    }

    // ========== 撤離倒數 ==========
    public static int getExtractionTimerSeconds() {
        return extractionTimerSeconds;
    }

    public static void setExtractionTimer(int seconds) {
        extractionTimerSeconds = Math.max(5, Math.min(120, seconds));
    }

    // ========== 獎勵倍率 ==========
    public static double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public static void setRewardMultiplier(double mult) {
        rewardMultiplier = Math.max(0.1, Math.min(10.0, mult));
    }
}
