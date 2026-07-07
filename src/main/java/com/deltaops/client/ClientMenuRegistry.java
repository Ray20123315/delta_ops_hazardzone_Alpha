/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.client;

import com.deltaops.DeltaOpsMod;
import com.deltaops.inventory.ModInventoryMenus;
import com.deltaops.inventory.WarehouseScreen;
import com.deltaops.inventory.SellScreen;
import com.deltaops.inventory.TradeScreen;
import com.deltaops.loot.AdminItemTaggingMenu;
import com.deltaops.loot.AdminItemTaggingScreen;
import com.deltaops.screen.ModMenuTypes;
import com.deltaops.screen.SecureBoxScreen;
import com.deltaops.shop.TraderScreen;
import com.deltaops.weapon.WeaponWorkbenchScreen;
import com.deltaops.combat.WeaponConfigScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientMenuRegistry {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(ModMenuTypes.SECURE_BOX.get(), SecureBoxScreen::new);
        MenuScreens.register(ModMenuTypes.ADMIN_ITEM_TAGGING.get(), AdminItemTaggingScreen::new);
        MenuScreens.register(ModMenuTypes.TRADER.get(), TraderScreen::new);
        MenuScreens.register(ModMenuTypes.WEAPON_WORKBENCH.get(), WeaponWorkbenchScreen::new);
        MenuScreens.register(ModMenuTypes.WEAPON_CONFIG.get(), WeaponConfigScreen::new);
        // 新庫存/交易系統
        MenuScreens.register(ModInventoryMenus.WAREHOUSE.get(), WarehouseScreen::new);
        MenuScreens.register(ModInventoryMenus.SELL.get(), SellScreen::new);
        MenuScreens.register(ModInventoryMenus.TRADE.get(), TradeScreen::new);
    }
}
