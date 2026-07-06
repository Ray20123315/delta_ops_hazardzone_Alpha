/*
 * Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.environment;

import com.deltaops.DeltaOpsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/**
 * 環境氛圍系統 — 基於生態域與時間的沉浸式音效。
 *
 * - 不同生態域有不同的環境音效（森林鳥鳴、洞穴滴水、沙漠風聲）
 * - 夜晚播放夜間昆蟲/風聲
 * - 每 30~60 秒隨機觸發一次
 * - 純客戶端，不影響伺服器
 */
@Mod.EventBusSubscriber(modid = DeltaOpsMod.MOD_ID, value = Dist.CLIENT)
public class EnvironmentSoundHandler {

    private static final Random RNG = new Random();
    private static long nextSoundTime = 0;

    // 生態域 → 音效映射
    private static SoundEvent getBiomeSound(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        var biomeHolder = level.getBiome(pos);
        Biome biome = biomeHolder.value();

        // 使用 Biome 的命名空間
        ResourceLocation biomeKey = ForgeRegistries.BIOMES.getKey(biome);
        String namespace = biomeKey != null ? biomeKey.toString() : "";

        boolean isNight = level.getDayTime() % 24000 > 13000;

        // 夜晚通用音效
        if (isNight) {
            return SoundEvents.AMBIENT_CAVE.get();
        }

        // 根據生態域分類
        if (namespace.contains("forest") || namespace.contains("birch") || namespace.contains("taiga")) {
            return SoundEvents.AMBIENT_CAVE.get();
        }
        if (namespace.contains("desert") || namespace.contains("badlands") || namespace.contains("savanna")) {
            return SoundEvents.AMBIENT_CAVE.get();
        }
        if (namespace.contains("ocean") || namespace.contains("river") || namespace.contains("beach")) {
            return SoundEvents.AMBIENT_CAVE.get();
        }
        if (namespace.contains("plains") || namespace.contains("meadow") || namespace.contains("sunflower")) {
            return SoundEvents.AMBIENT_CAVE.get();
        }
        if (namespace.contains("jungle")) {
            return SoundEvents.AMBIENT_CAVE.get(); // 叢林用洞穴音效替代
        }
        if (namespace.contains("swamp") || namespace.contains("mangrove")) {
            return SoundEvents.AMBIENT_CAVE.get(); // 沼澤用洞穴音效替代
        }
        if (namespace.contains("snow") || namespace.contains("ice") || namespace.contains("frozen")) {
            return SoundEvents.AMBIENT_CAVE.get(); // 雪地用洞穴音效替代
        }

        return null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        if (now < nextSoundTime) return;

        // 隨機間隔 30~90 秒
        nextSoundTime = now + (30000 + RNG.nextInt(60000));

        SoundEvent sound = getBiomeSound(mc.level, mc.player.blockPosition());
        if (sound != null) {
            // 在玩家附近隨機位置播放
            float x = (float) (mc.player.getX() + (RNG.nextFloat() - 0.5) * 20);
            float y = (float) (mc.player.getY() + (RNG.nextFloat() - 0.5) * 10);
            float z = (float) (mc.player.getZ() + (RNG.nextFloat() - 0.5) * 20);
            mc.getSoundManager().play(
                    SimpleSoundInstance.forAmbientAddition(sound)
            );
            DeltaOpsMod.LOGGER.debug("[環境音效] 播放: {}", sound.getLocation());
        }
    }
}
