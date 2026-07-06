/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.hud;

import com.deltaops.DeltaOpsMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 物品工具提示：顯示物品價值（哈夫幣價格）與稀有度顏色。
 *
 * - 從 EconomyManager 取得物品價格
 * - 根據價格範圍顯示不同稀有度顏色
 * - 價格越高稀有度越高
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, value = Dist.CLIENT)
public class RarityTooltipHandler {

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.US);

    /** 稀有度層級 */
    public enum Rarity {
        COMMON("§7普通", ChatFormatting.GRAY, 0),
        UNCOMMON("§a不凡", ChatFormatting.GREEN, 100),
        RARE("§9稀有", ChatFormatting.BLUE, 1000),
        EPIC("§5史詩", ChatFormatting.DARK_PURPLE, 10000),
        LEGENDARY("§6傳說", ChatFormatting.GOLD, 50000),
        MYTHIC("§c§l神話", ChatFormatting.RED, 200000);

        public final String display;
        public final ChatFormatting color;
        public final long minPrice;

        Rarity(String display, ChatFormatting color, long minPrice) {
            this.display = display;
            this.color = color;
            this.minPrice = minPrice;
        }

        public static Rarity fromPrice(long price) {
            Rarity result = COMMON;
            for (Rarity r : values()) {
                if (price >= r.minPrice) result = r;
            }
            return result;
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // 取得物品價格
        long price = com.deltaops.lobby.EconomyManager.getItemPrice(stack);
        Rarity rarity = Rarity.fromPrice(price);

        // 移除第一行原版物品名稱，用自訂稀有度顏色取代
        if (!event.getToolTip().isEmpty()) {
            // 修改第一行（物品名稱）加上稀有度顏色
            MutableComponent name = Component.literal("")
                    .append(Component.literal(rarity.color.toString()))
                    .append(stack.getHoverName().copy());
            event.getToolTip().set(0, name);
        }

        // 在第二行插入稀有度標籤和價格
        String priceStr = price > 0 ? " §7$" + FMT.format(price) : "";
        event.getToolTip().add(1, Component.literal(
                rarity.display + "§8 |§7 價值: " + (price > 0 ? "§6$" + FMT.format(price) : "§7無價值")
        ));

        // 如果是安全箱物品，加上特殊標記
        if (stack.hasTag() && stack.getTag().contains("delta_secure_box_item")) {
            event.getToolTip().add(Component.literal("§8🔒 安全箱物品"));
        }

        // 如果是武器，加上戰備值
        if (com.deltaops.combat.WeaponDetection.isGun(stack)) {
            event.getToolTip().add(Component.literal("§c⚔ 武器 §7| 戰備價值: §6$" + FMT.format(price)));
        }
    }
}
