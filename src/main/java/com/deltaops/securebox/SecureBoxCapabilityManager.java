package com.deltaops.securebox;

import com.deltaops.DeltaOpsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class SecureBoxCapabilityManager {
    private static final String TAG = "SecureBoxData";
    private static final String TAG_LEVEL = "SecureBoxLevel";
    private static final String TAG_UNLOCKED = "SecureBoxUnlocked";

    // 各等級安全箱的價格
    public static final long[] PURCHASE_PRICES = { 0L, 10000L, 50000L, 150000L, 500000L }; // index = level, Lv1=10000, Lv4=500000

    /**
     * 玩家是否已解鎖安全箱。
     */
    public static boolean isUnlocked(ServerPlayer player) {
        if (player == null) return false;
        return player.getPersistentData().getBoolean(TAG_UNLOCKED);
    }

    /**
     * 解鎖或升級安全箱（扣除哈夫幣）。
     * @return true 如果購買成功
     */
    public static boolean purchase(ServerPlayer player, int targetLevel) {
        if (player == null) return false;
        int currentLevel = getSecureBoxLevel(player);
        if (targetLevel <= currentLevel) return false; // 不能降級購買
        if (targetLevel < 1 || targetLevel > 4) return false;

        long price = PURCHASE_PRICES[targetLevel];
        long balance = com.deltaops.lobby.EconomyManager.getBalance(player);
        if (balance < price) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c餘額不足！需要 " + price + " 哈夫幣（目前: " + balance + "）。"));
            return false;
        }

        com.deltaops.lobby.EconomyManager.addBalance(player, -price);
        setSecureBoxLevel(player, targetLevel);
        player.getPersistentData().putBoolean(TAG_UNLOCKED, true);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a✅ 安全箱已解鎖至 Lv" + targetLevel + "！（花費 " + price + " 哈夫幣）"));
        return true;
    }

    public static ItemStackHandler getSecureBoxHandler(ServerPlayer player) {
        if (player == null) {
            return new ItemStackHandler(2);
        }

        int level = getSecureBoxLevel(player);
        int slots = getSlotCount(level);
        ItemStackHandler handler = new ItemStackHandler(slots);
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        if (tag.contains("Items", Tag.TAG_COMPOUND)) {
            handler.deserializeNBT(tag.getCompound("Items"));
        }
        return handler;
    }

    public static void saveSecureBoxHandler(ServerPlayer player, ItemStackHandler handler) {
        if (player == null || handler == null) {
            return;
        }

        CompoundTag tag = new CompoundTag();
        tag.put("Items", handler.serializeNBT());
        tag.putInt(TAG_LEVEL, getSecureBoxLevel(player));
        tag.putBoolean(TAG_UNLOCKED, isUnlocked(player));
        player.getPersistentData().put(TAG, tag);
    }

    public static int getSecureBoxLevel(ServerPlayer player) {
        if (player == null) {
            return 1;
        }

        CompoundTag data = player.getPersistentData();
        int level = Math.max(1, Math.min(4, data.getInt("SecureBoxLevel")));
        // 檢查是否已解鎖，若未解鎖但等級 > 0 則視為等級 0（未解鎖狀態）
        if (!data.getBoolean(TAG_UNLOCKED)) {
            return level; // 仍回傳等級，但外部會檢查 isUnlocked
        }
        return level;
    }

    public static void setSecureBoxLevel(ServerPlayer player, int level) {
        if (player == null) return;
        player.getPersistentData().putInt("SecureBoxLevel", Math.max(1, Math.min(4, level)));
    }

    public static void copySecureBoxToNewPlayer(Player oldPlayer, Player newPlayer) {
        if (oldPlayer == null || newPlayer == null) return;

        CompoundTag oldTag = oldPlayer.getPersistentData().getCompound(TAG);
        CompoundTag newTag = new CompoundTag();
        newTag.put("Items", oldTag.getCompound("Items"));
        newTag.putInt(TAG_LEVEL, oldTag.getInt(TAG_LEVEL));
        newTag.putBoolean(TAG_UNLOCKED, oldTag.getBoolean(TAG_UNLOCKED));
        newPlayer.getPersistentData().put(TAG, newTag);
    }

    public static int getSlotCount(int level) {
        return switch (level) {
            case 2 -> 4;
            case 3 -> 6;
            case 4 -> 9;
            default -> 2;
        };
    }
}

