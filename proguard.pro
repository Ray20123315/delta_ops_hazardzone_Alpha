#
# ProGuard 混淆配置 — Delta Ops: Hazard Zone
# 用於保護核心代碼，防止反編譯與篡改
#

# ============================================================
# 1. 保留所有 Minecraft/Forge/FML 入口 — 否則遊戲崩潰
# ============================================================

# Forge mod 主類別（@Mod 註解）
-keep @net.minecraftforge.fml.common.Mod class * { *; }

# Mod 事件總線訂閱者（@Mod.EventBusSubscriber）
-keep @net.minecraftforge.fml.common.Mod.EventBusSubscriber class * { *; }

# Forge 模組初始化事件監聽
-keep class * {
    @net.minecraftforge.eventbus.api.SubscribeEvent <methods>;
}

# Mixin 類別（若有）
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }

# ============================================================
# 2. 保留 ForgeGradle 重新混淆所需的結構
# ============================================================

-keep class * implements net.minecraftforge.fml.common.Mod { *; }
-keep class * extends net.minecraftforge.registries.DeferredRegister { *; }
-keep class * implements net.minecraftforge.network.simple.SimpleChannel { *; }

# 保留所有網路封包類別（encode/decode/handle 透過反射調用）
-keep class com.deltaops.network.** { *; }
-keepclassmembers class com.deltaops.network.** {
    public static void encode(*, *);
    public static * decode(*);
    public static void handle(*, *);
}

# ============================================================
# 3. 保留 Screen / MenuType 註冊（透過 ClientRegistry 調用）
# ============================================================

-keep class com.deltaops.screen.** { *; }
-keep class * extends net.minecraft.client.gui.screens.Screen { *; }
-keep class * extends net.minecraft.world.inventory.AbstractContainerMenu {
    public <init>(int, net.minecraft.world.entity.player.Inventory);
    public <init>(int, net.minecraft.world.entity.player.Inventory, *);
}
-keepclassmembers class * extends net.minecraft.world.inventory.AbstractContainerMenu {
    *** ITEM_MAP;
}

# ============================================================
# 4. 保留自訂方塊 / 物品 / BlockEntity 註冊
# ============================================================

-keep class com.deltaops.block.** { *; }
-keep class com.deltaops.item.** { *; }
-keep class * extends net.minecraft.world.level.block.Block { *; }
-keep class * extends net.minecraft.world.item.Item { *; }
-keep class * extends net.minecraft.world.level.block.entity.BlockEntity { *; }

# ============================================================
# 5. 保留 Config / Capability / 序列化相關
# ============================================================

-keep class com.deltaops.config.** { *; }
-keep class * extends net.minecraftforge.common.ForgeConfigSpec { *; }

# 保留 Capability token（泛型擦除後反射仍需要）
-keepclassmembers class * {
    @net.minecraftforge.common.capabilities.CapabilityInject <fields>;
}

# ============================================================
# 6. 保留 Gson 序列化用的數據類別
# ============================================================

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.deltaops.** {
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.deltaops.zone.Zone { *; }
-keep class com.deltaops.lobby.MapDefinition { *; }
-keep class com.deltaops.loot.LootTag { *; }

# ============================================================
# 7. 保留安全模組中透過反射調用的方法
# ============================================================

-keep class com.deltaops.security.** { *; }

# ============================================================
# 8. 通用保留規則
# ============================================================

# 保留列舉
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留所有 public main 方法（Jar 入口）
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# ============================================================
# 9. 混淆設定 — com.deltaops 全部混淆為 a, b, c...
# ============================================================

# 注意：不需要 -keep class com.deltaops，因為我們就是要把它們混淆掉
# 以下只保留必要的入口，其餘全數混淆

-repackageclasses 'o'
-allowaccessmodification
-overloadaggressively
-useuniqueclassmembernames
-verbose
-dontoptimize   # 關閉優化（避免與 dontshrink 衝突）
-dontshrink     # 不縮減（保留所有 class，僅混淆名稱）
-dontwarn       # 忽略 library 警告（Minecraft/Forge 非標準結構）
-ignorewarnings # 繼續執行即使有警告

# 翻新 SourceFile 屬性，防止透過錯誤訊息還原結構
-renamesourcefileattribute SourceFile
-keepattributes Exceptions, InnerClasses, Signature, Deprecated,
                SourceFile, LineNumberTable, *Annotation*, EnclosingMethod

# 輸出 mapping 方便除錯追蹤（路徑由 build.gradle 的 -printmapping 指定）

