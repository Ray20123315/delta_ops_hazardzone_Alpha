package com.deltaops.screen;

import com.deltaops.DeltaOpsMod;
import com.deltaops.admin.AdminConfigMenu;
import com.deltaops.container.TacticalContainerMenu;
import com.deltaops.loot.AdminItemTaggingMenu;
import com.deltaops.securebox.SecureBoxCapabilityManager;
import com.deltaops.securebox.SecureBoxMenu;
import com.deltaops.container.ContainerVariant;
import com.deltaops.shop.TraderMenu;
import com.deltaops.weapon.WeaponWorkbenchMenu;
import com.deltaops.combat.WeaponConfigMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DeltaOpsMod.MOD_ID);

    public static final RegistryObject<MenuType<SecureBoxMenu>> SECURE_BOX =
            MENU_TYPES.register("secure_box", () -> IForgeMenuType.create((id, inv, data) -> {
                if (inv.player instanceof ServerPlayer serverPlayer) {
                    return new SecureBoxMenu(id, inv, SecureBoxCapabilityManager.getSecureBoxHandler(serverPlayer));
                }
                return new SecureBoxMenu(id, inv, new ItemStackHandler(2));
            }));

    public static final RegistryObject<MenuType<TacticalContainerMenu>> TACTICAL_CONTAINER =
            MENU_TYPES.register("tactical_container", () -> IForgeMenuType.create((id, inv, data) -> new TacticalContainerMenu(id, inv, ContainerVariant.LARGE_SAFE, new ItemStackHandler(9), new ItemStackHandler(0), false)));

    public static final RegistryObject<MenuType<AdminItemTaggingMenu>> ADMIN_ITEM_TAGGING =
            MENU_TYPES.register("admin_item_tagging", () -> IForgeMenuType.create((id, inv, data) -> new AdminItemTaggingMenu(id, inv)));

    public static final RegistryObject<MenuType<AdminConfigMenu>> ADMIN_CONFIG =
            MENU_TYPES.register("admin_config", () -> IForgeMenuType.create((id, inv, data) -> new AdminConfigMenu(id, inv)));

    public static final RegistryObject<MenuType<TraderMenu>> TRADER =
            MENU_TYPES.register("trader", () -> IForgeMenuType.create((id, inv, data) -> new TraderMenu(id, inv)));

    public static final RegistryObject<MenuType<WeaponWorkbenchMenu>> WEAPON_WORKBENCH =
            MENU_TYPES.register("weapon_workbench", () -> IForgeMenuType.create((id, inv, data) -> new WeaponWorkbenchMenu(id, inv)));

    public static final RegistryObject<MenuType<WeaponConfigMenu>> WEAPON_CONFIG =
            MENU_TYPES.register("weapon_config", () -> IForgeMenuType.create((id, inv, data) -> new WeaponConfigMenu(id, inv)));
}