## Delta Ops Hazard Zone v0.2.1 (Pre-release)

### 🐛 崩潰修復
- 修復 ProGuard 混淆導致 `ContainerVariant`（實作 `StringRepresentable`）的 `getSerializedName()` 方法（SRG: `m_7912_()`）被 ProGuard 改名，Forge 運行時因找不到對應的介面方法而拋出 `AbstractMethodError`。

### 🔧 解決方案
- 在 `proguard.pro` 中加入 `-keep class com.deltaops.container.ContainerVariant { *; }`，保留完整類別及其所有方法名稱（包含 SRG 名稱），確保與 Forge 運行時的方法映射一致。

### ✅ 驗證
- 透過 `mapping.txt` 確認 `m_7912_() -> m_7912_` 被正確保留，無名稱變更。

> ⚠️ 此為 Pre-release 版本，尚未完整測試，僅供開發與測試用途。
