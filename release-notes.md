## Delta Ops Hazard Zone v0.1.0 (Pre-release)

### 新增
- **武器系統** — WeaponWorkbench、WeaponAttachment、WeaponConfig 完整架構
- **任務系統** — QuestManager、QuestData、任務指令與 GUI
- **商店系統** — TraderMenu/TraderScreen、購買交易封包
- **安全系統** — HMAC 配置驗證、簽章服務、程式碼完整性檢查、診斷指令
- **同步機制** — SyncConfigBroadcaster / SyncConfigPacket 伺服器→客戶端配置同步
- **玩家血量管理** — PlayerHealthManager（部位傷害取代舊 BodyPartHealth）
- **熱鍵欄限制** — HotbarRestrictionHandler + ScrollWheelHandler
- **武器限制** — WeaponLimitHandler、WeaponDetection
- **連線配對** — MatchmakingScreen
- **HUD 血量覆層** — HealthOverlayHandler

### 修正
- 修復多項編譯錯誤（ModCreativeTab、LootCrateBlockEntity 等）

### 技術
- Gradle build 最佳化

> ⚠️ 此為 Pre-release 版本，尚未完整測試，僅供開發與測試用途。
