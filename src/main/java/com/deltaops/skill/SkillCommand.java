/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.skill;

import com.deltaops.DeltaOpsMod;
import com.deltaops.lobby.LobbySquadManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 技能系統指令：/dt skills 開啟技能面板
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class SkillCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("dt")
                .then(Commands.literal("skills")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();

                            // 準備中禁止查看技能
                            if (LobbySquadManager.isPlayerReady(player)) {
                                player.sendSystemMessage(Component.literal("§c準備狀態下無法查看技能！請先取消準備。"));
                                return 0;
                            }

                            player.sendSystemMessage(Component.literal("§e=== 技能面板 ==="));
                            player.sendSystemMessage(Component.literal("§7技能點數：§6" + SkillManager.getSkillPoints(player)));
                            for (var entry : SkillManager.SKILLS.entrySet()) {
                                String id = entry.getKey();
                                SkillManager.SkillDef def = entry.getValue();
                                int level = SkillManager.getSkillLevel(player, id);
                                String desc = level > 0 && level <= def.descriptions().length
                                        ? def.descriptions()[level - 1] : def.descriptions()[0];
                                player.sendSystemMessage(Component.literal(
                                        "§f" + def.name() + " §7(Lv." + level + "/" + def.maxLevel() + ") §8- " + desc));
                            }
                            player.sendSystemMessage(Component.literal("§e輸入 §6/dt skillup <技能ID> §e消耗 1 點升級"));
                            return 1;
                        }))
                .then(Commands.literal("skillup")
                        .then(Commands.argument("skillId", StringArgumentType.word())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    // 準備中禁止升級技能
                                    if (LobbySquadManager.isPlayerReady(player)) {
                                        player.sendSystemMessage(Component.literal("§c準備狀態下無法升級技能！請先取消準備。"));
                                        return 0;
                                    }
                                    String skillId = StringArgumentType.getString(context, "skillId");
                                    if (!SkillManager.SKILLS.containsKey(skillId)) {
                                        player.sendSystemMessage(Component.literal("§c未知技能 ID！可用技能：" + String.join(", ", SkillManager.SKILLS.keySet())));
                                        return 0;
                                    }
                                    if (SkillManager.upgradeSkill(player, skillId)) {
                                        SkillManager.SkillDef def = SkillManager.SKILLS.get(skillId);
                                        int level = SkillManager.getSkillLevel(player, skillId);
                                        player.sendSystemMessage(Component.literal("§a技能「" + def.name() + "」已升至 Lv." + level + "！"));
                                    } else {
                                        player.sendSystemMessage(Component.literal("§c升級失敗！技能點不足或已達最高等級。"));
                                    }
                                    return 1;
                                }))));
    }
}
