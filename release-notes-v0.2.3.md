## Delta Ops Hazard Zone v0.2.3 (Pre-release)

### 🐛 `/kill` 無效修復
- 大廳保護 `LobbyProtectionManager` 先前全面攔截所有傷害類型，包含 `/kill` 指令用的 `outOfWorld` 傷害來源
- 新增例外判斷：允許 `outOfWorld` 傷害來源通過，管理員在大廳中也能正常使用 `/kill` 指令進行除錯

### 🖥️ 血量顯示數位化
- 取消原版愛心條渲染，改用數字直接顯示「❤ 目前HP/最大HP」
- 使用 `RenderGuiOverlayEvent.Pre` 攔截 `PLAYER_HEALTH` 重繪，不受 maxHealth 同步延遲影響
- 根據血量百分比（紅 >80%、橙 60~80%、金 40~60%、綠 20~40%、灰 <20%）自動變換愛心顏色

### 🔧 其他改進
- `VanillaHudHider` 不再重複取消血量渲染（改由 `HealthOverlayHandler` 直接攔截）
- 移除已廢棄的 `SURGERY_KIT`、`PAINKILLER` 物品引用
- 清理自訂愛心貼圖渲染的舊程式碼

> ⚠️ 此為 Pre-release 版本，尚未完整測試，僅供開發與測試用途。
