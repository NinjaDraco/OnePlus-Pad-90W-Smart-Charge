package com.oneplus.smartpower.data

data class DeviceTelemetry(
    val batteryTempC: Float = 31.5f,
    val cpuTempC: Float = 36.2f,
    val powerWatts: Float = 3.2f,
    val batteryLevel: Int = 85,
    val isCharging: Boolean = false,
    val chargeStatusText: String = "未充电 (正常放电)",
    val gpuFreqMhz: Int = 160,
    val cpuLittleFreqMhz: Int = 1958,
    val cpuPrimeFreqMhz: Int = 2246,
    val activeProfileText: String = "日常均衡 (120Hz)",
    val thermalState: ThermalLevel = ThermalLevel.NORMAL
)

enum class ThermalLevel(val title: String, val desc: String) {
    NORMAL("正常", "温度在安全范围 (<40°C)"),
    WARM("轻微温热", "一阶自动防积热 (40°C~44°C)"),
    HOT("高温预警", "二阶主动控频控温 (44°C~48°C)"),
    CRITICAL("极高过热", "三阶安全熔断防护 (>48°C)")
}

data class GameItem(
    val packageName: String,
    val appName: String,
    val isCasual: Boolean, // true = 休闲轻游戏 (GPU 525MHz/CPU 2.24GHz, 3W防过热); false = 重载电竞 (120Hz 极速)
    val isCustom: Boolean = false
)

enum class GameMode {
    NONE, CASUAL, HEAVY
}

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isGame: Boolean = false,
    val mode: GameMode = GameMode.NONE
)

data class PowerConfig(
    val autoThermalThrottle: Boolean = true,
    val fastChargeEnabled: Boolean = false,
    val tempThresholdWarn: Float = 42.0f,
    val tempThresholdHot: Float = 46.0f,
    val tempThresholdCrit: Float = 48.0f
)
