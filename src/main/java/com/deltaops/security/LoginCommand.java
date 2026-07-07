/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.security;

import com.deltaops.DeltaOpsMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 管理員解鎖指令 /logintoken <密碼>。
 *
 * 流程：
 * 1. 接收玩家輸入的密碼
 * 2. 將密碼與 SALT 拼接後進行 SHA-256 加密，轉為 Hex 字串 (clientToken)
 * 3. 取得當前模組 Hash
 * 4. 發送 register 請求至 Worker（Java 端不判斷密碼對錯，交由 Worker 判斷）
 * 5. 若 Worker 回傳 SUCCESS，則將 {@link VerificationManager#isVerified} 設為 true
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class LoginCommand {

    /**
     * 鹽值（SALT）：用於密碼混淆加密。
     * Java 端不做密碼比對，僅將 (密碼 + SALT) 進行 SHA-256 後傳送給 Worker 判斷。
     */
    private static final String SALT = "qwertyuiop[]\\1234567890-=!@#$%^&*()_+asdfghjkl;'zxcvbnm,./ZXCVBNM<>?ASDFGHJKL:\"QWERTYUIOP{}|~!@#$%^&*()_+";

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("logintoken")
                .then(Commands.argument("password", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String password = StringArgumentType.getString(ctx, "password");

                            // 1. 將密碼與 SALT 拼接
                            String salted = password + SALT;

                            // 2. SHA-256 加密 → Hex 字串
                            String clientToken = sha256Hex(salted);

                            // 3. 取得當前模組 Hash
                            String hash = VerificationManager.getModuleHash();

                            // 4. 發送 register 請求至 Worker（Java 端不判斷密碼對錯）
                            DeltaOpsMod.LOGGER.info("🔐 [LoginCommand] 玩家 {} 嘗試解鎖...", player.getName().getString());
                            boolean success = NetworkClient.sendRegister(clientToken, hash);

                            // 5. 根據 Worker 回傳結果處理
                            if (success) {
                                VerificationManager.isVerified = true;
                                player.sendSystemMessage(Component.literal(""));
                                player.sendSystemMessage(Component.literal("§a§m-----------------------------------------------------"));
                                player.sendSystemMessage(Component.literal("§a✅ 解鎖成功！模組功能已完全開放。"));
                                player.sendSystemMessage(Component.literal("§a§m-----------------------------------------------------"));
                                player.sendSystemMessage(Component.literal(""));
                                DeltaOpsMod.LOGGER.info("✅ [LoginCommand] 玩家 {} 解鎖成功", player.getName().getString());
                            } else {
                                player.sendSystemMessage(Component.literal("§c❌ 解鎖失敗，請確認密碼是否正確，或聯絡開發者。"));
                                DeltaOpsMod.LOGGER.warn("⚠️ [LoginCommand] 玩家 {} 解鎖失敗（密碼錯誤或 Worker 拒絕）", player.getName().getString());
                            }

                            return 1;
                        }))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal("§c用法: /logintoken <密碼>"));
                    return 0;
                }));
    }

    /**
     * 計算 SHA-256 並回傳小寫 Hex 字串。
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            DeltaOpsMod.LOGGER.error("⚠️ [LoginCommand] SHA-256 加密失敗", e);
            return "";
        }
    }
}
