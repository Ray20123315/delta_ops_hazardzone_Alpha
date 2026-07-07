/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.inventory;

import com.deltaops.DeltaOpsMod;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 註冊倉庫與交易相關的 MenuType
 */
public class ModInventoryMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DeltaOpsMod.MOD_ID);

    public static final RegistryObject<MenuType<WarehouseMenu>> WAREHOUSE =
            MENU_TYPES.register("warehouse", () -> IForgeMenuType.create(WarehouseMenu::new));

    public static final RegistryObject<MenuType<SellMenu>> SELL =
            MENU_TYPES.register("sell", () -> IForgeMenuType.create(SellMenu::new));

    public static final RegistryObject<MenuType<TradeMenu>> TRADE =
            MENU_TYPES.register("trade", () -> IForgeMenuType.create(TradeMenu::new));

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
