# OnePlus Pad Pro 90W 智能极速充与低功耗 + 540Hz 触控优化模块

专为 **OnePlus Pad Pro / OnePlus Pad 2 (骁龙 8 至尊版 SM8750 / 骁龙 8 Gen 3)** 定制的 Magisk / KernelSU / APatch 模块。

---

## ⚡ 核心特性

### 1. ⚡ 90W SUPERVOOC / PPS 满血智能极速闪充
- 自动解锁系统级 `single_speed_charge_state` 极速充电开关。
- **全新 v2.1.0 桌面独立开关（配套 APK 与控制中心磁贴）**：
  - 解决 ColorOS 亮屏强制降流至 35W~40W 的底层限制。
  - 配套 **`FastChargeToggle.apk`**：桌面独立开关图标，点击一下即开即关，**0 应用界面**，弹出原生 Toast 提示。
  - 同时支持**下拉控制中心快捷磁贴（QS Tile）**，打游戏时下拉即可切换。
- **智能阶梯温控策略**：
  - **默认模式（智能控温）**：
    - `< 42°C`：解锁 `cool_down` 限制，释放 90W 极速满血闪充；
    - `42°C ~ 45°C`：切换至智能平衡温控档位（约 55W-65W 恒温速充）；
    - `> 45°C`：激活安全回退保护电芯健康，避免过热。
  - **满血狂暴模式（桌面开关激活）**：
    - 强制锁定 0 档温控，亮屏不降速，电量极速爬升；
    - 内置 48°C 硬件级极限自熔断保护，防止硬件损伤。

### 2. 🎮 540Hz 电竞级硬件触控采样与超低延迟
- 写入 `/proc/touchpanel/game_switch_enable` 与超频节点。
- `sys.input.resample.latency=0` 实现 0ms 触控重采样延迟。
- 优化触控响应阻尼与灵敏度 (`view.touch_slop=2`)，显著提升跟手度与滑动顺滑感。

### 3. 🎬 视频与日常场景动态能效调度
- **主流视频场景**（B站、YouTube、主流媒体播放器）：自动锁定 60Hz 动态刷新率，压制超大核虚高频率，大幅降低视频播放功耗。
- **日常使用场景**：保持 120Hz/144Hz 流畅交互与极速触控。
- **息屏待机场景**：自动切入深度 Doze 休眠，清理无效唤醒，显著提升待机续航。

---

## 📦 安装与使用方法

> [!IMPORTANT]
> **请在 [Releases 发布页面](https://github.com/NinjaDraco/OnePlus-Pad-90W-Smart-Charge/releases) 或本仓库 `release/` 目录下载打包好的文件！**
> 1. **Magisk 模块刷机包**：`release/OnePlus_Pad_90W_Smart_Charge_v2.1.0.zip`
> 2. **桌面开关 APK**：`release/FastChargeToggle.apk`（安装后桌面出现「⚡满血快充」开关）
> 
> 切勿直接点击 GitHub 网页顶部的绿色 `Code -> Download ZIP`，网页打包源码带有多余顶层文件夹会导致 Magisk 报错 `specified file not found in archive`。

### 步骤 1：刷入 Magisk / KSU 模块
1. 下载 `OnePlus_Pad_90W_Smart_Charge_v2.1.0.zip`。
2. 打开 **Magisk** / **KernelSU** / **APatch** 管理器 -> 模块 -> 从本地安装。
3. 安装完成后重启平板。

### 步骤 2：安装桌面独立开关（可选）
1. 下载并安装 `FastChargeToggle.apk`。
2. 安装后桌面会出现 **`⚡满血快充`** 图标。
3. **点击第 1 次**：提示 `⚡【满血快充已开启】已解除亮屏与温控限制！`。
4. **点击第 2 次**：提示 `🛡️【智能温控已恢复】已关闭满血快充，恢复默认保护策略`。
5. （可选）下拉顶部控制中心，点击右上角编辑，可将「满血快充」磁贴拖入控制面板，实现随时随地下拉盲操切换。

---

## 🔍 状态监控

开机后，模块后台守护进程会自动向 `/data/local/tmp/smart_power_status.json` 输出实时监控指标：
```json
{
  "charge_state": "⚡ 90W 极速闪充 (满血输出)",
  "power_profile": "📱 日常流畅均衡 (120Hz 动态流畅)",
  "touch_sampling_mode": "⚡ 540Hz 极速跟手触控 (已激活)",
  "battery_level": 88,
  "battery_temp": "37.0"
}
```

---

## ⚠️ 免责声明
本模块仅供交流学习使用。请确保使用官方或合规快充充电器及数据线。
