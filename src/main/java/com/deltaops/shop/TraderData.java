/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.shop;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 商人商品資料。管理員可透過 JSON 或程式方式擴充商品。
 */
public class TraderData {
    private static final List<TraderItem> ITEMS = new ArrayList<>();

    static {
        // 安全箱（特殊商品，購買後解鎖安全箱功能）
        ITEMS.add(new TraderItem(new ItemStack(Items.ENDER_CHEST), 1, 10000, true, 1));   // Lv1 安全箱
        ITEMS.add(new TraderItem(new ItemStack(Items.ENDER_CHEST), 1, 50000, true, 2));    // Lv2 安全箱
        ITEMS.add(new TraderItem(new ItemStack(Items.ENDER_CHEST), 1, 150000, true, 3));   // Lv3 安全箱
        ITEMS.add(new TraderItem(new ItemStack(Items.ENDER_CHEST), 1, 500000, true, 4));   // Lv4 安全箱

        // 一般商品
        ITEMS.add(new TraderItem(new ItemStack(Items.BREAD), 5, 50, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.COOKED_BEEF), 3, 120, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.ARROW), 16, 80, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.BOW), 1, 500, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.IRON_SWORD), 1, 1500, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.IRON_CHESTPLATE), 1, 2000, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.IRON_PICKAXE), 1, 800, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.IRON_AXE), 1, 1000, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.TORCH), 32, 100, false, 0));
        ITEMS.add(new TraderItem(new ItemStack(Items.ENDER_PEARL), 1, 3000, false, 0));
    }

    public static List<TraderItem> getItems() {
        return Collections.unmodifiableList(ITEMS);
    }

    public record TraderItem(ItemStack itemStack, int stackSize, long price, boolean isSecureBox, int secureBoxLevel) {}
}
