# OnePlus Pad Pro 90W 智能极速充与低功耗 + 540Hz 触控优化模块

专为 **OnePlus Pad Pro / OnePlus Pad 2 (骁龙 8 至尊版 SM8750 / 骁龙 8 Gen 3)** 定制的 Magisk / KernelSU / APatch 模块。

---

## ✨ 核心特性

### 1. ⚡ 90W SUPERVOOC / PPS 满血智能极速闪充
- 自动解锁系统级 `single_speed_charge_state` 极速充电开关。
- 智能阶梯温控策略：
  - **< 42°C**：解开 cool_down 限制，释放 90W 极速满血闪充；
  - **42°C ~ 45°C**：切换至智能平衡温控档位（约 55W-65W 恒温速充）；
  - **> 45°C**：激活安全回退保护电芯健康，避免过热。

### 2. 🎮 540Hz 电竞级硬件触控采样与超低延迟
- 写入 `/proc/touchpanel/game_switch_enable` 与超频节点。
- `sys.input.resample.latency=0` 0ms 触控重采样延迟。
- 优化触控响应阻尼与灵敏度 (`view.touch_slop=2`)，提升跟手度与滑动顺滑感。

### 3. 🎬 视频与日常场景动态能效调度
- **主流视频场景**（B站、YouTube、主流媒体播放器）：自动锁定 60Hz 动态刷新率，压制超大核虚高频率，大幅降低视频播放功耗。
- **日常使用场景**：保持 120Hz 流畅交互与极速触控。
- **息屏待机场景**：自动切入深度 Doze 休眠，清理无效唤醒，显著提升待机续航。

---

## 📦 安装方法

> [!IMPORTANT]
> **请务必在 [Releases 发布页面](https://github.com/NinjaDraco/OnePlus-Pad-90W-Smart-Charge/releases) 下载打包好的 `OnePlus_Pad_90W_Smart_Charge.zip` 刷机包！**
> ❌ **切勿点击 GitHub 网页顶部的绿色 `Code -> Download ZIP`**。网页下载的源码压缩包带有多余的顶层目录文件夹，在 Magisk / KernelSU / APatch 中刷入会提示 `- Error: specified file not found in archive (Error code: 1)` 报错！

1. 前往本仓库的 [Releases 页面](https://github.com/NinjaDraco/OnePlus-Pad-90W-Smart-Charge/releases) 下载最新的 `OnePlus_Pad_90W_Smart_Charge.zip`。
2. 打开 **Magisk** / **KernelSU** / **APatch** 管理器。
3. 进入「模块」页面，选择「从本地安装」，选择下载好的 `OnePlus_Pad_90W_Smart_Charge.zip`。
4. 安装完成后重启平板即可生效。

---

## 🔍 状态监控

开机后，模块后台守护进程会自动在 `/data/local/tmp/smart_power_status.json` 输出实时监控指标：
```json
{
  "charge_state": "⚡ 90W 极速闪充 (满血输出)",
  "power_profile": "📱 日常流畅均衡 (120Hz 动态流利)",
  "touch_sampling_mode": "⚡ 540Hz 极速跟手触控 (已激活)",
  "battery_level": 88,
  "battery_temp": "37.0"
}
```

---

## ⚠️ 免责声明
本模块仅供交流学习使用。请确保使用官方或合规快充充电器及数据线。
