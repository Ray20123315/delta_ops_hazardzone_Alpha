/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.security;

import com.deltaops.DeltaOpsMod;
import com.deltaops.lobby.EconomyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/**
 * /dt diag 診斷指令
 *
 * 用法:
 *   /dt diag                    - 輸出完整診斷資訊
 *   /dt diag sign <filename>    - 對指定設定檔手動 HMAC 簽署蓋章
 *   /dt config sign <filename>  - 同上（別名）
 *   /dt diag verify <filename>  - 校驗指定設定檔的簽名狀態
 *   /dt diag dev add <uuid>     - 新增開發者 UUID
 *   /dt diag dev remove <uuid>  - 移除開發者 UUID
 *   /dt diag dev list           - 列出開發者 UUID
 *
 * 權限：雙通道驗證 — OP Level 2 或 deltaops.developer 權限節點，
 *       或玩家 UUID 在 developer_uuids 白名單中。
 *
 * 日誌規範：任何呼叫此診斷指令的行為，必須強制寫入伺服器日誌。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class DiagnosticCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        // ===== /dt config sign (別名) + /dt diag 主指令 =====
        d.register(Commands.literal("dt")
                .then(Commands.literal("config")
                        .requires(DiagnosticCommand::hasPermissionOrDeveloper)
                        .then(Commands.literal("sign")
                                .then(Commands.argument("filename", StringArgumentType.string())
                                        .executes(ctx -> executeSign(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "filename"))))))
                .then(Commands.literal("diag")
                        .requires(DiagnosticCommand::hasPermissionOrDeveloper)
                        // --- /dt diag (唯讀診斷報告) ---
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();

                            // 強制寫入伺服器日誌
                            logDiagnosticAccess(src);

                            StringBuilder sb = new StringBuilder();
                            sb.append("§6╔══════════════════════════════════════════════╗\n");
                            sb.append("§6║     Delta Ops: Hazard Zone 診斷報告       ║\n");
                            sb.append("§6╚══════════════════════════════════════════════╝\n");

                            // 模組資訊
                            sb.append("§e模組 ID: §f").append(DeltaOpsMod.MOD_ID).append("\n");

                            // 第一層：代碼完整性狀態
                            CodeIntegrityValidator.IntegrityStatus status = CodeIntegrityValidator.getStatus();
                            String statusColor;
                            switch (status) {
                                case VALID: statusColor = "§a"; break;
                                case INVALID: statusColor = "§c"; break;
                                case ERROR: statusColor = "§e"; break;
                                default: statusColor = "§7";
                            }
                            sb.append("§e第一層防線（代碼完整性）: ").append(statusColor).append(status);
                            String ver = CodeIntegrityValidator.getVerifiedVersion();
                            if (!ver.isEmpty()) {
                                sb.append(" §7(v").append(ver).append(")");
                            }
                            sb.append("\n");
                            if (!CodeIntegrityValidator.getLastErrorMessage().isEmpty()) {
                                sb.append("§c  原因: §f").append(CodeIntegrityValidator.getLastErrorMessage()).append("\n");
                            }

                            // 升級通知
                            if (CodeIntegrityValidator.hasUpgradeNotice()) {
                                sb.append("§e升級通知: §c⚠ 偵測到舊版模組！\n");
                            }

                            // 第二層：HMAC 設定檔狀態
                            Path configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                                    .resolve(DeltaOpsMod.MOD_ID);
                            sb.append("§e第二層防線（設定檔簽名）:\n");
                            sb.append("§e  設定檔目錄: §f").append(configDir.toAbsolutePath()).append("\n");
                            // 列出所有 JSON 設定檔
                            try {
                                if (Files.exists(configDir)) {
                                    try (var files = Files.list(configDir)) {
                                        files.filter(f -> f.toString().endsWith(".json"))
                                                .forEach(f -> {
                                                    String name = f.getFileName().toString();
                                                    boolean hmacOk = com.deltaops.security.HMACConfigManager.verifyConfig(f);
                                                    sb.append("  ").append(hmacOk ? "§a✔" : "§c✘")
                                                            .append(" §f").append(name).append("\n");
                                                });
                                    }
                                }
                            } catch (IOException ignored) {}

                            // 經濟系統
                            sb.append("§e物價項目數: §f").append(EconomyManager.getItemCount()).append("\n");

                            // 玩家資訊
                            try {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                if (player != null) {
                                    sb.append("§e玩家: §f").append(player.getScoreboardName())
                                            .append(" §7(UUID: ").append(player.getUUID()).append(")\n");
                                    sb.append("§e餘額: §f$").append(EconomyManager.getBalance(player)).append("\n");
                                    sb.append("§e權限: ")
                                            .append(player.hasPermissions(2) ? "§aOP Level 2+" : "§7一般玩家")
                                            .append(" | ")
                                            .append(DeveloperSettings.isDeveloper(player.getUUID()) ? "§a開發者" : "§7非開發者")
                                            .append("\n");
                                }
                            } catch (Exception ignored) {}

                            sb.append("§6========================================");

                            src.sendSystemMessage(Component.literal(sb.toString()));
                            return 1;
                        })
                        // --- /dt diag sign <filename> ---
                        .then(Commands.literal("sign")
                                .then(Commands.argument("filename", StringArgumentType.string())
                                        .executes(ctx -> executeSign(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "filename")))))
                        // --- /dt diag verify <filename> ---
                        .then(Commands.literal("verify")
                                .then(Commands.argument("filename", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String filename = StringArgumentType.getString(ctx, "filename");
                                            Path configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                                                    .resolve(DeltaOpsMod.MOD_ID);
                                            Path targetFile = configDir.resolve(filename);

                                            if (!Files.exists(targetFile)) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("§c找不到檔案: " + targetFile.toAbsolutePath()));
                                                return 0;
                                            }

                                            boolean valid = HMACConfigManager.verifyConfig(targetFile);
                                            ctx.getSource().sendSystemMessage(
                                                    Component.literal((valid ? "§a" : "§c")
                                                            + filename + " 簽名驗證: " + (valid ? "§a通過" : "§c失敗（可能被篡改）")));
                                            return valid ? 1 : 0;
                                        })))
                        // --- /dt diag dev add/list/remove ---
                        .then(Commands.literal("dev")
                                .requires(DiagnosticCommand::hasPermissionOrDeveloper)
                                .then(Commands.literal("add")
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    String uuidStr = StringArgumentType.getString(ctx, "uuid");
                                                    try {
                                                        UUID uuid = UUID.fromString(uuidStr);
                                                        DeveloperSettings.addDeveloper(uuid);
                                                        DeltaOpsMod.LOGGER.info("[Delta Ops] 開發者 UUID 已新增: {} (由 {})",
                                                                uuidStr, ctx.getSource().getTextName());
                                                        ctx.getSource().sendSystemMessage(
                                                                Component.literal("§a已新增開發者: " + uuidStr));
                                                    } catch (IllegalArgumentException e) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§c無效的 UUID 格式"));
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("uuid", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    String uuidStr = StringArgumentType.getString(ctx, "uuid");
                                                    try {
                                                        UUID uuid = UUID.fromString(uuidStr);
                                                        DeveloperSettings.removeDeveloper(uuid);
                                                        DeltaOpsMod.LOGGER.info("[Delta Ops] 開發者 UUID 已移除: {} (由 {})",
                                                                uuidStr, ctx.getSource().getTextName());
                                                        ctx.getSource().sendSystemMessage(
                                                                Component.literal("§a已移除開發者: " + uuidStr));
                                                    } catch (IllegalArgumentException e) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§c無效的 UUID 格式"));
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("list")
                                        .executes(ctx -> {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append("§6=== 開發者 UUID 列表 ===\n");
                                            Path devFile = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                                                    .resolve(DeltaOpsMod.MOD_ID).resolve("developer.json");
                                            try {
                                                if (Files.exists(devFile)) {
                                                    sb.append(Files.readString(devFile));
                                                } else {
                                                    sb.append("§7(無開發者設定)");
                                                }
                                            } catch (IOException e) {
                                                sb.append("§c讀取失敗: ").append(e.getMessage());
                                            }
                                            ctx.getSource().sendSystemMessage(Component.literal(sb.toString()));
                                            return 1;
                                        })))));
    }

    /**
     * 執行 /dt diag sign 或 /dt config sign。
     * 手動 HMAC 簽署蓋章 + 載入並廣播同步。
     */
    private static int executeSign(CommandSourceStack src, String filename) {
        Path configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve(DeltaOpsMod.MOD_ID);
        Path targetFile = configDir.resolve(filename);

        if (!Files.exists(targetFile)) {
            src.sendFailure(Component.literal("§c找不到檔案: " + targetFile.toAbsolutePath()));
            return 0;
        }

        // 產生 HMAC 簽名
        HMACConfigManager.signConfig(targetFile);
        DeltaOpsMod.LOGGER.info("[Delta Ops] 設定檔已手動簽署: {} (由 {})", filename, src.getTextName());

        // 重新載入設定（如果是物價表）
        if (filename.contains("item_prices")) {
            com.deltaops.lobby.EconomyManager.reloadPrices();
            src.sendSystemMessage(Component.literal("§a已為 " + filename + " 簽署並重新載入物價表"));
        } else {
            src.sendSystemMessage(Component.literal("§a已為 " + filename + " 產生 HMAC 簽名"));
        }

        // 廣播同步給所有線上玩家
        com.deltaops.network.sync.SyncConfigBroadcaster.broadcast();

        return 1;
    }

    /**
     * 雙通道權限驗證：
     * - OP Level 2 以上
     * - deltaops.developer 權限節點
     * - developer_uuids 白名單
     */
    private static boolean hasPermissionOrDeveloper(CommandSourceStack src) {
        // OP Level 2 或以上
        if (src.hasPermission(2)) return true;

        // 開發者 UUID 白名單
        try {
            ServerPlayer player = src.getPlayerOrException();
            // OP 檢查 (via server whitelist/ops)
            if (player.hasPermissions(2)) return true;
            // 開發者 UUID 白名單
            if (DeveloperSettings.isDeveloper(player.getUUID())) return true;
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * 強制寫入伺服器日誌，記錄診斷指令的呼叫。
     */
    private static void logDiagnosticAccess(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            DeltaOpsMod.LOGGER.info("[Delta Ops] 診斷指令被呼叫 - 玩家: {} (UUID: {})",
                    player.getScoreboardName(), player.getUUID());
        } catch (Exception e) {
            DeltaOpsMod.LOGGER.info("[Delta Ops] 診斷指令被呼叫 - 來源: {}", src.getTextName());
        }
    }
}
