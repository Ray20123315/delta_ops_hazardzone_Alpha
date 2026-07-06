## Delta Ops Hazard Zone v0.2.2 (Pre-release)

### 🖥️ HUD 重構
- 愛心渲染改為 `RenderSystem.setShaderColor` 著色，保留原版愛心形狀（不再顯示方塊）
- 始終一排 10 顆心，不同血量層級用顏色區分（紅→橙→金→綠→灰），不換排不擋畫面

### 🍖 移除飢餓值
- 新增 `GlobalHungerFiller` 伺服端每 tick 補滿飢餓值，徹底移除飢餓機制
- 新增 `VanillaHudHider` 客戶端隱藏原版食物條與愛心條（由自訂渲染取代）

### 🔧 其他改進
- 大廳保護邏輯簡化，僅在大廳範圍內才補滿飢餓值
- 移除創造模式管理相關指令說明

> ⚠️ 此為 Pre-release 版本，尚未完整測試，僅供開發與測試用途。
