package com.deltaops;

import com.deltaops.block.ModBlocks;
import com.deltaops.combat.DeathPenaltyHandler;
import com.deltaops.config.ModConfig;
import com.deltaops.network.ModNetwork;
import com.deltaops.screen.ModMenuTypes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DeltaOpsMod.MOD_ID)
public class DeltaOpsMod {
    public static final String MOD_ID = "delta_ops_hazardzone";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DeltaOpsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, ModConfig.SPEC);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        com.deltaops.inventory.ModInventoryMenus.register(modEventBus);
        com.deltaops.item.ModItems.ITEMS.register(modEventBus);
        ModNetwork.register();
        com.deltaops.loot.GlobalLootDatabase.getInstance().load();
        com.deltaops.lobby.EconomyManager.init();

        LOGGER.info("=========== Delta Ops: Hazard Zone 已成功載入！ ===========");

        // 非同步啟動核心代碼完整性驗證
        com.deltaops.security.CodeIntegrityValidator.startValidation();
    }
}