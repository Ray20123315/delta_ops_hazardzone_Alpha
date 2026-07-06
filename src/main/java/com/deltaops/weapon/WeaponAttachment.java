/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 武器改裝系統 — 透過 NBT 為物品附加配件。
 * 支援瞄準鏡、槍口、握把、彈匣等配件類型。
 */
public class WeaponAttachment {

    public enum AttachmentType {
        SCOPE("scope"),
        MUZZLE("muzzle"),
        GRIP("grip"),
        MAGAZINE("magazine"),
        STOCK("stock");

        private final String tagName;
        AttachmentType(String tagName) { this.tagName = tagName; }
        public String getTagName() { return tagName; }

        public static AttachmentType fromTagName(String name) {
            for (AttachmentType t : values()) {
                if (t.tagName.equals(name)) return t;
            }
            return null;
        }
    }

    /**
     * 為物品附加配件。
     */
    public static ItemStack attach(ItemStack weapon, AttachmentType type, String attachmentId) {
        if (weapon.isEmpty()) return weapon;
        CompoundTag tag = weapon.getOrCreateTag();
        tag.putString("weapon_attachment_" + type.getTagName(), attachmentId);
        return weapon;
    }

    /**
     * 移除指定類型的配件。
     */
    public static ItemStack removeAttachment(ItemStack weapon, AttachmentType type) {
        if (weapon.isEmpty()) return weapon;
        String key = "weapon_attachment_" + type.getTagName();
        if (weapon.getTag() != null) {
            weapon.getTag().remove(key);
        }
        return weapon;
    }

    /**
     * 取得已安裝的配件 ID。
     */
    public static String getAttachment(ItemStack weapon, AttachmentType type) {
        if (weapon.isEmpty() || weapon.getTag() == null) return "";
        return weapon.getTag().getString("weapon_attachment_" + type.getTagName());
    }

    /**
     * 取得所有已安裝配件。
     */
    public static Map<AttachmentType, String> getAllAttachments(ItemStack weapon) {
        Map<AttachmentType, String> result = new LinkedHashMap<>();
        if (weapon.isEmpty() || weapon.getTag() == null) return result;
        for (AttachmentType type : AttachmentType.values()) {
            String val = weapon.getTag().getString("weapon_attachment_" + type.getTagName());
            if (!val.isEmpty()) {
                result.put(type, val);
            }
        }
        return result;
    }

    /**
     * 是否有已安裝的配件。
     */
    public static boolean hasAnyAttachment(ItemStack weapon) {
        return !getAllAttachments(weapon).isEmpty();
    }

    // ========== 配件資料庫 ==========

    public record AttachmentDef(String id, String displayName, AttachmentType type, long price, String description) {}

    private static final List<AttachmentDef> ALL_ATTACHMENTS = new ArrayList<>();

    static {
        // 瞄準鏡
        ALL_ATTACHMENTS.add(new AttachmentDef("red_dot", "紅點瞄準鏡", AttachmentType.SCOPE, 3000, "提升近距離精準度"));
        ALL_ATTACHMENTS.add(new AttachmentDef("holographic", "全息瞄準鏡", AttachmentType.SCOPE, 5000, "中等距離精準度"));
        ALL_ATTACHMENTS.add(new AttachmentDef("x4_scope", "4倍瞄準鏡", AttachmentType.SCOPE, 12000, "遠距離精準度"));

        // 槍口
        ALL_ATTACHMENTS.add(new AttachmentDef("silencer", "消音器", AttachmentType.MUZZLE, 8000, "消除開火聲音"));
        ALL_ATTACHMENTS.add(new AttachmentDef("compensator", "補償器", AttachmentType.MUZZLE, 4000, "減少後座力"));

        // 握把
        ALL_ATTACHMENTS.add(new AttachmentDef("foregrip", "前握把", AttachmentType.GRIP, 3500, "減少垂直後座力"));
        ALL_ATTACHMENTS.add(new AttachmentDef("angled_grip", "直角握把", AttachmentType.GRIP, 4500, "減少水平後座力"));

        // 彈匣
        ALL_ATTACHMENTS.add(new AttachmentDef("extended_mag", "擴容彈匣", AttachmentType.MAGAZINE, 6000, "增加彈容量"));
        ALL_ATTACHMENTS.add(new AttachmentDef("fast_mag", "快速彈匣", AttachmentType.MAGAZINE, 4000, "加快換彈速度"));

        // 槍托
        ALL_ATTACHMENTS.add(new AttachmentDef("light_stock", "輕型槍托", AttachmentType.STOCK, 3000, "提升機動性"));
        ALL_ATTACHMENTS.add(new AttachmentDef("heavy_stock", "重型槍托", AttachmentType.STOCK, 5000, "減少後座力"));
    }

    public static List<AttachmentDef> getAllAttachmentDefs() {
        return Collections.unmodifiableList(ALL_ATTACHMENTS);
    }

    public static List<AttachmentDef> getAttachmentsByType(AttachmentType type) {
        return ALL_ATTACHMENTS.stream().filter(a -> a.type() == type).toList();
    }

    public static AttachmentDef getAttachmentDef(String id) {
        return ALL_ATTACHMENTS.stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
    }
}
