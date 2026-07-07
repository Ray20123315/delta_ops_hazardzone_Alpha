/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import com.deltaops.DeltaOpsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 交易系統管理器：處理交易請求與開啟交易 GUI
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID)
public class TradeManager {

    private static final Map<UUID, UUID> TRADE_REQUESTS = new HashMap<>(); // target -> requester

    /** 發送交易請求 */
    public static void sendTradeRequest(ServerPlayer requester, ServerPlayer target) {
        if (requester == null || target == null) return;
        if (requester.getUUID().equals(target.getUUID())) {
            requester.sendSystemMessage(Component.literal("§c不能與自己交易！"));
            return;
        }
        TRADE_REQUESTS.put(target.getUUID(), requester.getUUID());
        target.sendSystemMessage(Component.literal("§e" + requester.getGameProfile().getName() + " 想與你交易！輸入 §6/dt trade accept §e接受"));
        requester.sendSystemMessage(Component.literal("§a已向 " + target.getGameProfile().getName() + " 發送交易請求。"));
    }

    /** 接受交易請求 */
    public static void acceptTrade(ServerPlayer player) {
        UUID requesterId = TRADE_REQUESTS.remove(player.getUUID());
        if (requesterId == null) {
            player.sendSystemMessage(Component.literal("§c你沒有待處理的交易請求。"));
            return;
        }
        ServerPlayer requester = player.getServer().getPlayerList().getPlayer(requesterId);
        if (requester == null) {
            player.sendSystemMessage(Component.literal("§c交易請求者已離線。"));
            return;
        }
        // 為雙方開啟交易 GUI
        openTradeForBoth(requester, player);
    }

    private static void openTradeForBoth(ServerPlayer p1, ServerPlayer p2) {
        // 建立共享的 trade handler
        TradeMenu.SharedTradeHandler handler = new TradeMenu.SharedTradeHandler(p1, p2);

        NetworkHooks.openScreen(p1, new MenuProvider() {
            @Override public Component getDisplayName() { return Component.literal("交易 - " + p2.getGameProfile().getName()); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                return new TradeMenu(id, inv, player, handler, false);
            }
        });

        NetworkHooks.openScreen(p2, new MenuProvider() {
            @Override public Component getDisplayName() { return Component.literal("交易 - " + p1.getGameProfile().getName()); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                return new TradeMenu(id, inv, player, handler, true);
            }
        });
    }

    /** 拒絕交易請求 */
    public static void denyTrade(ServerPlayer player) {
        UUID requesterId = TRADE_REQUESTS.remove(player.getUUID());
        if (requesterId == null) {
            player.sendSystemMessage(Component.literal("§c你沒有待處理的交易請求。"));
            return;
        }
        ServerPlayer requester = player.getServer().getPlayerList().getPlayer(requesterId);
        if (requester != null) {
            requester.sendSystemMessage(Component.literal("§c" + player.getGameProfile().getName() + " 拒絕了你的交易請求。"));
        }
        player.sendSystemMessage(Component.literal("§c已拒絕交易請求。"));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        d.register(net.minecraft.commands.Commands.literal("dt")
                .then(net.minecraft.commands.Commands.literal("trade")
                        .then(net.minecraft.commands.Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer requester = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                    sendTradeRequest(requester, target);
                                    return 1;
                                }))
                        .then(net.minecraft.commands.Commands.literal("accept")
                                .executes(ctx -> {
                                    acceptTrade(ctx.getSource().getPlayerOrException());
                                    return 1;
                                }))
                        .then(net.minecraft.commands.Commands.literal("deny")
                                .executes(ctx -> {
                                    denyTrade(ctx.getSource().getPlayerOrException());
                                    return 1;
                                }))));
    }

    /** 右鍵點擊玩家觸發交易 */
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // 不處理手持物品的互動，避免干擾
    }
}
