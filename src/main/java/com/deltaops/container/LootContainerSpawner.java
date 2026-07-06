/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.container;

import com.deltaops.block.ModBlocks;
import com.deltaops.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.deltaops.zone.ZoneSelectionManager;
import com.deltaops.zone.ZoneSelectionManager.Zone;
import com.deltaops.zone.ZoneSelectionManager.SearchPriority;
import com.deltaops.block.ModBlocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public class LootContainerSpawner {
    private static final Random RANDOM = new Random();

    /**
     * 清除指定 AABB 範圍內所有 TacticalContainer，
     * 並將該位置還原為空氣。
     */
    public static void clearAllContainersInZone(ServerLevel level, AABB zone) {
        if (level == null || zone == null) return;
        int minX = (int) Math.floor(zone.minX);
        int minY = (int) Math.floor(zone.minY);
        int minZ = (int) Math.floor(zone.minZ);
        int maxX = (int) Math.ceil(zone.maxX);
        int maxY = (int) Math.ceil(zone.maxY);
        int maxZ = (int) Math.ceil(zone.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModBlocks.TACTICAL_CONTAINER.get())) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    public static void spawnFixedHighValue(ServerLevel level, BlockPos pos, ContainerVariant variant) {
        if (level == null || pos == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.is(Blocks.WATER) && !state.is(Blocks.LAVA)) {
            return;
        }

        level.setBlockAndUpdate(pos, ModBlocks.TACTICAL_CONTAINER.get().defaultBlockState().setValue(TacticalContainerBlock.VARIANT, variant));
        if (level.getBlockEntity(pos) instanceof TacticalContainerBlockEntity entity) {
            entity.setVariant(variant);
            if (variant.supportsHiddenLayer()) {
                entity.setHiddenLayerUnlocked(true);
            }
            entity.populateLootIfEmpty();
        }
    }

    public static void spawnFromZones(ServerLevel level) {
        if (level == null) return;
        java.util.Map<String, Zone> zones = ZoneSelectionManager.loadZonesMap();
        for (Zone z : zones.values()) {
            BlockPos min = z.min();
            BlockPos max = z.max();
            int count = switch (z.priority) {
                case HIGH_PRIORITY -> 64;
                case MID_PRIORITY -> 24;
                case LOW_PRIORITY -> 8;
                default -> 12;
            };
            for (int i = 0; i < count; i++) {
                int x = min.getX() + RANDOM.nextInt(Math.max(1, max.getX() - min.getX() + 1));
                int zc = min.getZ() + RANDOM.nextInt(Math.max(1, max.getZ() - min.getZ() + 1));
                BlockPos candidate = new BlockPos(x, max.getY(), zc);
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, candidate);
                BlockPos spawn = surface.below();
                if (spawn.getY() <= min.getY() || spawn.getY() > max.getY()) continue;
                if (!level.getBlockState(spawn).isAir()) continue;

                ContainerVariant variant;
                if (z.priority == SearchPriority.HIGH_PRIORITY) {
                    variant = RANDOM.nextBoolean() ? ContainerVariant.PREMIUM_BOX : ContainerVariant.SERVER;
                } else if (z.priority == SearchPriority.MID_PRIORITY) {
                    variant = RANDOM.nextBoolean() ? ContainerVariant.WEAPON_CASE : ContainerVariant.TRAVEL_BAG;
                } else if (z.priority == SearchPriority.LOW_PRIORITY) {
                    variant = ContainerVariant.TRASH_CAN;
                } else {
                    variant = ContainerVariant.WEAPON_CASE;
                }

                level.setBlockAndUpdate(spawn, ModBlocks.TACTICAL_CONTAINER.get().defaultBlockState().setValue(TacticalContainerBlock.VARIANT, variant));
                if (level.getBlockEntity(spawn) instanceof TacticalContainerBlockEntity entity) {
                    entity.setVariant(variant);
                    entity.populateLootIfEmpty();
                }
            }
        }
    }

    public static void spawnRandomNormal(ServerLevel level, BlockPos center, int radius) {
        if (level == null || center == null) {
            return;
        }

        List<ContainerVariant> variants = new ArrayList<>();
        variants.add(ContainerVariant.WEAPON_CASE);
        variants.add(ContainerVariant.MEDICAL_BAG);
        variants.add(ContainerVariant.AMMO_BOX);
        variants.add(ContainerVariant.TOOLBOX);
        variants.add(ContainerVariant.HIKING_BAG);
        variants.add(ContainerVariant.TRAVEL_BAG);
        variants.add(ContainerVariant.LOCKER);
        variants.add(ContainerVariant.PC_CASE);
        variants.add(ContainerVariant.DRAWER);
        variants.add(ContainerVariant.FIELD_CRATE);
        variants.add(ContainerVariant.HIDDEN_STASH);
        variants.add(ContainerVariant.TRASH_CAN);
        variants.add(ContainerVariant.BIRD_NEST);
        variants.add(ContainerVariant.CEMENT_MIXER);
        variants.add(ContainerVariant.EXPRESS_BOX);
        variants.add(ContainerVariant.CAR_STORAGE);
        variants.add(ContainerVariant.DESERT_CHEST);

        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = RANDOM.nextInt(radius * 2 + 1) - radius;
            int dz = RANDOM.nextInt(radius * 2 + 1) - radius;
            BlockPos candidate = center.offset(dx, 0, dz);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, candidate);
            if (surface.getY() <= 0) {
                continue;
            }

            BlockPos spawnPos = surface.below();
            BlockState state = level.getBlockState(spawnPos);
            if (!state.isAir()) {
                continue;
            }

            ContainerVariant variant = variants.get(RANDOM.nextInt(variants.size()));
            level.setBlockAndUpdate(spawnPos, ModBlocks.TACTICAL_CONTAINER.get().defaultBlockState().setValue(TacticalContainerBlock.VARIANT, variant));
            if (level.getBlockEntity(spawnPos) instanceof TacticalContainerBlockEntity entity) {
                entity.setVariant(variant);
            }
            break;
        }
    }
}
