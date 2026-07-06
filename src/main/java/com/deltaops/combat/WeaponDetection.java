/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 武器與戰鬥物品檢測工具。
 * 用於判斷物品是否為 TaCZ 槍械、近戰武器、投擲物等。
 */
public class WeaponDetection {

    /**
     * 判斷是否為戰鬥物品（可放入快捷欄）：
     * 使用者自訂的槍械 + 劍、斧、弓、投擲物
     */
    public static boolean isCombatItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();

        // 使用者自訂的槍械
        if (WeaponConfig.isWeapon(stack)) return true;

        // 原版武器/工具
        if (item instanceof SwordItem) return true;
        if (item instanceof AxeItem) return true;
        if (item instanceof BowItem) return true;
        if (item instanceof CrossbowItem) return true;
        if (item instanceof TridentItem) return true;
        if (item instanceof ShieldItem) return true;

        // 投擲物
        if (item instanceof SnowballItem) return true;
        if (item instanceof EggItem) return true;
        if (item instanceof ThrowablePotionItem) return true;
        if (item == Items.ENDER_PEARL) return true;
        if (item instanceof ExperienceBottleItem) return true;
        if (item == Items.FIRE_CHARGE) return true;

        return false;
    }

    /**
     * 判斷是否為槍械（使用者自訂）。
     */
    public static boolean isGun(ItemStack stack) {
        return WeaponConfig.isWeapon(stack);
    }

    /**
     * 判斷是否為近戰武器（劍、斧）。
     */
    public static boolean isMeleeWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem;
    }

    /**
     * 判斷是否為投擲物。
     */
    public static boolean isThrowable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof SnowballItem || item instanceof EggItem ||
               item instanceof ThrowablePotionItem || item == Items.ENDER_PEARL ||
               item instanceof ExperienceBottleItem || item == Items.FIRE_CHARGE;
    }
}
