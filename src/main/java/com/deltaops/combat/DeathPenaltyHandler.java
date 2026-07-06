package com.deltaops.combat;

import com.deltaops.DeltaOpsMod;
import com.deltaops.config.ModConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathPenaltyHandler {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        event.getDrops().clear();
        event.setCanceled(true);

        String rule = ModConfig.getDeathDropRule();
        if (rule.equals("NONE")) {
            return; // 完全不掉落
        }
        if (rule.equals("INVENTORY_ONLY")) {
            dropInventoryOnly(player);
        } else {
            dropAllItems(player);
        }
    }

    private static void dropAllItems(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                dropStack(player, stack.copy());
                player.getInventory().items.set(i, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (!stack.isEmpty()) {
                dropStack(player, stack.copy());
                player.getInventory().armor.set(i, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            ItemStack stack = player.getInventory().offhand.get(i);
            if (!stack.isEmpty()) {
                dropStack(player, stack.copy());
                player.getInventory().offhand.set(i, ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
    }

    private static void dropInventoryOnly(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                dropStack(player, stack.copy());
                player.getInventory().items.set(i, ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
    }

    private static void dropStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        Vec3 pos = player.position();
        ItemEntity entity = new ItemEntity(player.level(), pos.x, pos.y + 0.5D, pos.z, stack);
        entity.setDefaultPickUpDelay();
        player.level().addFreshEntity(entity);
    }
}
