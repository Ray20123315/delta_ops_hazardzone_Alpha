/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.security;

import com.deltaops.DeltaOpsMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 診斷測試指令 /dt test。
 * 逐一執行所有 /dt 子指令，回報各指令是否正常可用。
 * 用於排查「試圖執行該命令時出現意外錯誤」的問題。
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class TestCommand {

    /** 所有 /dt 子指令測試（含子參數版本） */
    private static final List<String> TEST_COMMANDS = List.of(
            // 獨立指令（無需參數）
            "dt menu",
            "dt balance",
            "dt shop",
            "dt quests",
            "dt skills",
            "dt stash",
            "dt sellgui",
            "dt sell",
            "dt weapons",
            "dt workbench",
            "dt diag",
            "dt leave",

            // dt zone 家族（bare 指令無 executor，用子指令測試即可）
            "dt zone pos1",
            "dt zone pos2",
            "dt zone generate",

            // dt login 家族
            "dt login test123456",

            // dt config 家族（需子參數）
            "dt config sign test.json",

            // dt apply / accept / kick（需玩家名稱或 UUID）
            "dt apply 00000000-0000-0000-0000-000000000000",
            "dt accept TestPlayer",
            "dt kick TestPlayer",

            // dt lobby 家族
            "dt lobby set",

            // dt spawn 家族
            "dt spawn add zero_dam",
            "dt spawn list zero_dam"
    );

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("dt")
                .then(Commands.literal("test")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.sendSystemMessage(Component.literal(
                                    "§6§m========================================"));
                            source.sendSystemMessage(Component.literal(
                                    "§6    Delta Ops 指令診斷測試"));
                            source.sendSystemMessage(Component.literal(
                                    "§6§m========================================"));

                            int total = 0;
                            int passed = 0;
                            int failed = 0;
                            List<String> errors = new ArrayList<>();

                            // 取得當前已註冊的 /dt 子節點
                            var dtNode = d.getRoot().getChildren().stream()
                                    .filter(n -> n.getName().equals("dt"))
                                    .findFirst().orElse(null);

                            if (dtNode == null) {
                                source.sendSystemMessage(Component.literal(
                                        "§c❌ 找不到 /dt 根節點！指令完全未註冊！"));
                                return 0;
                            }

                            // 列出所有已註冊的子節點
                            StringBuilder registered = new StringBuilder("§7已註冊的子指令: ");
                            for (var child : dtNode.getChildren()) {
                                registered.append("§f").append(child.getName()).append("§7, ");
                            }
                            source.sendSystemMessage(Component.literal(registered.toString()));
                            source.sendSystemMessage(Component.literal(""));

                            // --- 測試前置準備：建立 test.json 供 dt config sign 使用 ---
                            Path configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                                    .resolve(DeltaOpsMod.MOD_ID);
                            Path testFile = configDir.resolve("test.json");
                            boolean testFileCreated = false;
                            try {
                                Files.createDirectories(configDir);
                                Files.writeString(testFile, "{\"test\": true}");
                                testFileCreated = true;
                            } catch (Exception e) {
                                DeltaOpsMod.LOGGER.warn("無法建立測試用設定檔: {}", e.getMessage());
                            }

                            // 逐一實際執行每個指令
                            for (String cmd : TEST_COMMANDS) {
                                total++;
                                try {
                                    int result = d.execute(cmd, source);
                                    if (result > 0) {
                                        passed++;
                                        source.sendSystemMessage(Component.literal(" §a✔ " + cmd));
                                    } else {
                                        failed++;
                                        String err = "§e" + cmd + " → 執行失敗 (回傳 " + result + ")";
                                        errors.add(err);
                                        source.sendSystemMessage(Component.literal(" §e⚠ " + err));
                                    }
                                    DeltaOpsMod.LOGGER.info("📋 [TestCommand] {} → result={}", cmd, result);
                                } catch (Exception e) {
                                    failed++;
                                    String msg = e.getMessage() != null ? e.getMessage() : "";
                                    if (e.getCause() != null && e.getCause().getMessage() != null)
                                        msg += " | cause: " + e.getCause().getMessage();
                                    String err = "§c" + cmd + " → " + e.getClass().getSimpleName() + ": " + msg;
                                    errors.add(err);
                                    source.sendSystemMessage(Component.literal(" §c✘ " + err));
                                    DeltaOpsMod.LOGGER.error("📋 [TestCommand] {} 執行失敗", cmd, e);
                                }
                            }

                            source.sendSystemMessage(Component.literal(""));
                            source.sendSystemMessage(Component.literal(
                                    "§6§m========================================"));
                            source.sendSystemMessage(Component.literal(
                                    "§6測試完成: §a" + passed + " 通過 §7/ §c" + failed + " 失敗 §7/ §e" + total + " 總計"));

                            // 如果有失敗，列出錯誤摘要
                            if (!errors.isEmpty()) {
                                source.sendSystemMessage(Component.literal("§c錯誤摘要:"));
                                for (String err : errors) {
                                    source.sendSystemMessage(Component.literal(" §7- " + err));
                                }
                            }

                            // --- 測試後清理：刪除測試用設定檔 ---
                            if (testFileCreated) {
                                try {
                                    Files.deleteIfExists(testFile);
                                } catch (Exception e) {
                                    DeltaOpsMod.LOGGER.warn("無法刪除測試用設定檔: {}", e.getMessage());
                                }
                            }

                            source.sendSystemMessage(Component.literal(
                                    "§6§m========================================"));

                            DeltaOpsMod.LOGGER.info("📋 [TestCommand] 診斷測試完成: {}/{} 通過", passed, total);
                            return failed == 0 ? 1 : 0;
                        })));
    }
}
