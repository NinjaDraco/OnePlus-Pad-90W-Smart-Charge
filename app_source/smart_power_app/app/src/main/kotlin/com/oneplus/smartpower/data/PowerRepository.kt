package com.oneplus.smartpower.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.math.abs

class PowerRepository {

    private val sdcardHeavyGames = File("/sdcard/game_whitelist.txt")
    private val sdcardCasualGames = File("/sdcard/game_casual_whitelist.txt")

    fun execRoot(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream, "UTF-8"))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            output.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun readSysfs(path: String, defaultVal: String = ""): String {
        return try {
            val file = File(path)
            if (file.canRead()) {
                file.readText().trim()
            } else {
                execRoot("cat $path")
            }
        } catch (e: Exception) {
            defaultVal
        }
    }

    fun getTelemetry(): DeviceTelemetry {
        // 1. Battery Temp (raw is 0.1 deg C)
        val battTempRaw = readSysfs("/sys/class/power_supply/battery/temp", "320").toIntOrNull() ?: 320
        val battTempC = battTempRaw / 10.0f

        // 2. Battery Status & Level
        val battStatus = readSysfs("/sys/class/power_supply/battery/status", "Discharging")
        val isCharging = battStatus.equals("Charging", ignoreCase = true)
        val battLevel = readSysfs("/sys/class/power_supply/battery/capacity", "85").toIntOrNull() ?: 85

        // 3. Power (Watts) = Current (uA) * Voltage (uV) / 10^12
        val currUa = readSysfs("/sys/class/power_supply/battery/current_now", "0").toLongOrNull() ?: 0L
        val voltUv = readSysfs("/sys/class/power_supply/battery/voltage_now", "3800000").toLongOrNull() ?: 3800000L
        val powerRawW = (abs(currUa) * voltUv) / 1000000000000.0f
        val powerWatts = String.format("%.2f", powerRawW).toFloatOrNull() ?: 3.2f

        // 4. CPU & GPU Frequencies
        val gpuFreqHz = readSysfs("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq", "160000000").toLongOrNull() ?: 160000000L
        val gpuFreqMhz = (gpuFreqHz / 1000000).toInt()

        val cpuLittleHz = readSysfs("/sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq", "1958400").toLongOrNull() ?: 1958400L
        val cpuLittleMhz = (cpuLittleHz / 1000).toInt()

        val cpuPrimeHz = readSysfs("/sys/devices/system/cpu/cpufreq/policy6/scaling_cur_freq", "2246400").toLongOrNull() ?: 2246400L
        val cpuPrimeMhz = (cpuPrimeHz / 1000).toInt()

        // 5. Thermal state evaluation
        val thermalLevel = when {
            battTempC >= 48.0f -> ThermalLevel.CRITICAL
            battTempC >= 44.0f -> ThermalLevel.HOT
            battTempC >= 40.0f -> ThermalLevel.WARM
            else -> ThermalLevel.NORMAL
        }

        val chargeText = if (isCharging) {
            "⚡ 极速充电中 (+$powerWatts W)"
        } else {
            "🔋 电池供电中 (-$powerWatts W)"
        }

        val profileText = when {
            gpuFreqMhz <= 525 && !isCharging -> "🟢 捕鱼轻游戏省电 (锁525MHz, ~3.5W)"
            gpuFreqMhz <= 607 && isCharging -> "⚡ 捕鱼充电温控安全锁 (锁607MHz)"
            gpuFreqMhz >= 734 -> "🎮 重载电竞狂暴模式 (120Hz 澎湃)"
            else -> "日常均衡流畅 (120Hz)"
        }

        return DeviceTelemetry(
            batteryTempC = battTempC,
            cpuTempC = battTempC + 4.5f,
            powerWatts = powerWatts,
            batteryLevel = battLevel,
            isCharging = isCharging,
            chargeStatusText = chargeText,
            gpuFreqMhz = gpuFreqMhz,
            cpuLittleFreqMhz = cpuLittleMhz,
            cpuPrimeFreqMhz = cpuPrimeMhz,
            activeProfileText = profileText,
            thermalState = thermalLevel
        )
    }

    val telemetryFlow: Flow<DeviceTelemetry> = flow {
        while (true) {
            emit(getTelemetry())
            delay(1200)
        }
    }.flowOn(Dispatchers.IO)

    fun writeRootFile(path: String, content: String) {
        try {
            val b64 = android.util.Base64.encodeToString(content.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            execRoot("echo $b64 | base64 -d > $path")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRealAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            when (packageName) {
                "com.yooyoo.dwby3d.mi" -> "电玩捕鱼 3D"
                "com.netease.newspike" -> "Blood Strike"
                "com.epicgames.fortnite" -> "Fortnite"
                "com.limelight" -> "Moonlight"
                else -> packageName
            }
        }
    }

    fun readWhitelists(): Pair<Set<String>, Set<String>> {
        val heavySet = mutableSetOf<String>()
        val casualSet = mutableSetOf<String>()

        val heavyText = try {
            if (sdcardHeavyGames.exists()) sdcardHeavyGames.readText() else execRoot("cat /sdcard/game_whitelist.txt")
        } catch (_: Exception) {
            execRoot("cat /sdcard/game_whitelist.txt")
        }
        val casualText = try {
            if (sdcardCasualGames.exists()) sdcardCasualGames.readText() else execRoot("cat /sdcard/game_casual_whitelist.txt")
        } catch (_: Exception) {
            execRoot("cat /sdcard/game_casual_whitelist.txt")
        }

        heavyText.lines().forEach {
            val line = it.trim()
            if (line.isNotEmpty() && !line.startsWith("#")) heavySet.add(line)
        }
        casualText.lines().forEach {
            val line = it.trim()
            if (line.isNotEmpty() && !line.startsWith("#")) casualSet.add(line)
        }
        return Pair(heavySet, casualSet)
    }

    fun loadConfiguredGames(context: Context): List<GameItem> {
        val (heavySet, casualSet) = readWhitelists()
        val result = mutableListOf<GameItem>()
        val allPkgs = LinkedHashSet<String>()

        // Default: If completely empty, add fishing game as casual
        if (heavySet.isEmpty() && casualSet.isEmpty()) {
            allPkgs.add("com.yooyoo.dwby3d.mi")
        } else {
            allPkgs.addAll(casualSet)
            allPkgs.addAll(heavySet)
        }

        for (pkg in allPkgs) {
            val isCasual = if (heavySet.isEmpty() && casualSet.isEmpty()) true else casualSet.contains(pkg)
            val name = getRealAppName(context, pkg)
            result.add(GameItem(packageName = pkg, appName = name, isCasual = isCasual))
        }
        return result
    }

    fun scanInstalledApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val (heavySet, casualSet) = readWhitelists()
        val apps = mutableListOf<InstalledApp>()
        val seen = mutableSetOf<String>()

        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList = pm.queryIntentActivities(launchIntent, 0)
        for (ri in resolveList) {
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName || seen.contains(pkg)) continue
            seen.add(pkg)

            val label = ri.loadLabel(pm).toString()
            val appInfo = ri.activityInfo.applicationInfo
            val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo.category == ApplicationInfo.CATEGORY_GAME
            } else false

            val mode = when {
                casualSet.contains(pkg) -> GameMode.CASUAL
                heavySet.contains(pkg) -> GameMode.HEAVY
                else -> GameMode.NONE
            }
            apps.add(InstalledApp(packageName = pkg, appName = label, isGame = isGame, mode = mode))
        }

        return apps.sortedWith(
            compareByDescending<InstalledApp> { it.mode != GameMode.NONE }
                .thenByDescending { it.isGame }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun setAppMode(packageName: String, mode: GameMode) {
        val (heavySet, casualSet) = readWhitelists()
        val newHeavy = heavySet.toMutableSet()
        val newCasual = casualSet.toMutableSet()

        newHeavy.remove(packageName)
        newCasual.remove(packageName)

        when (mode) {
            GameMode.CASUAL -> newCasual.add(packageName)
            GameMode.HEAVY -> newHeavy.add(packageName)
            GameMode.NONE -> { /* removed from both */ }
        }

        writeRootFile("/sdcard/game_whitelist.txt", newHeavy.joinToString("\n"))
        writeRootFile("/sdcard/game_casual_whitelist.txt", newCasual.joinToString("\n"))
    }

    fun saveGameList(games: List<GameItem>) {
        val heavyLines = StringBuilder()
        val casualLines = StringBuilder()

        for (g in games) {
            if (g.isCasual) {
                casualLines.append(g.packageName).append("\n")
            } else {
                heavyLines.append(g.packageName).append("\n")
            }
        }

        writeRootFile("/sdcard/game_whitelist.txt", heavyLines.toString())
        writeRootFile("/sdcard/game_casual_whitelist.txt", casualLines.toString())
    }

    fun setFastCharge(enabled: Boolean) {
        val state = if (enabled) 0 else 1
        execRoot("settings put system single_speed_charge_state $state")
        execRoot("echo 0 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null")
    }

    fun applyDynamicThermal(telemetry: DeviceTelemetry, config: PowerConfig) {
        if (!config.autoThermalThrottle) return

        val temp = telemetry.batteryTempC
        when {
            temp >= config.tempThresholdCrit -> {
                // Critical (>48C): Force deep throttle
                execRoot("echo 1958400 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null")
                execRoot("echo 2246400 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null")
                execRoot("echo 525000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null")
                execRoot("echo 2 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null")
            }
            temp >= config.tempThresholdHot -> {
                // Hot (44C~48C): Mid throttle
                execRoot("echo 2227200 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null")
                execRoot("echo 2438400 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null")
                execRoot("echo 607000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null")
                execRoot("echo 1 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null")
            }
            temp >= config.tempThresholdWarn -> {
                // Warm (42C~44C): Gentle clamp
                execRoot("echo 2400000 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null")
                execRoot("echo 2841600 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null")
                execRoot("echo 734000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null")
            }
        }
    }
}
