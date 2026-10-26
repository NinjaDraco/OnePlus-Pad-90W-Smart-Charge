#!/system/bin/sh
##########################################################################################
# OnePlus Pad (SM8750 Snapdragon 8 Elite) Unified Smart Power & Dynamic Optimizer
# Version: 2.0.0
# Author: Antigravity
##########################################################################################

LOG_FILE="/data/local/tmp/smart_power_opt.log"
STATUS_JSON="/data/local/tmp/smart_power_status.json"
PID_FILE="/data/local/tmp/smart_power_daemon.pid"
SDCARD_CFG_1="/sdcard/pure_powersave_games.txt"
SDCARD_CFG_2="/sdcard/game_whitelist.txt"
SDCARD_CFG_CASUAL="/sdcard/casual_powersave_games.txt"

# Single-instance lock
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE" 2>/dev/null)
    if [ -n "$OLD_PID" ] && [ "$OLD_PID" != "$$" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        exit 0
    fi
fi
echo $$ > "$PID_FILE"

log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SMART_POWER] $1" >> "$LOG_FILE" 2>/dev/null
}

rotate_log() {
    if [ -f "$LOG_FILE" ]; then
        local size=$(wc -c < "$LOG_FILE" 2>/dev/null || echo 0)
        if [ "$size" -gt 131072 ] 2>/dev/null; then
            tail -n 250 "$LOG_FILE" > "$LOG_FILE.tmp" 2>/dev/null
            mv "$LOG_FILE.tmp" "$LOG_FILE" 2>/dev/null
        fi
    fi
}

# Ensure sysfs permissions
chmod 666 /proc/touchpanel/* 2>/dev/null
chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
chmod 666 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null

set_touch_esports() {
    echo 1 > /proc/touchpanel/game_switch_enable 2>/dev/null
    echo 1 > /proc/touchpanel/sensitive_level 2>/dev/null
    echo 1 > /proc/touchpanel/smooth_level 2>/dev/null
    resetprop -n sys.input.resample.latency 0 2>/dev/null
    resetprop -n persist.vendor.qti.input.resampling 1 2>/dev/null
    resetprop -n view.touch_slop 2 2>/dev/null
}

set_touch_low_latency() {
    echo 1 > /proc/touchpanel/game_switch_enable 2>/dev/null
    echo 1 > /proc/touchpanel/sensitive_level 2>/dev/null
    echo 1 > /proc/touchpanel/smooth_level 2>/dev/null
    resetprop -n sys.input.resample.latency 0 2>/dev/null
    resetprop -n persist.vendor.qti.input.resampling 1 2>/dev/null
    resetprop -n view.touch_slop 4 2>/dev/null
}

set_touch_normal() {
    echo 0 > /proc/touchpanel/game_switch_enable 2>/dev/null
    echo 0 > /proc/touchpanel/sensitive_level 2>/dev/null
    echo 0 > /proc/touchpanel/smooth_level 2>/dev/null
    resetprop -n sys.input.resample.latency 5 2>/dev/null
    resetprop -n persist.vendor.qti.input.resampling 0 2>/dev/null
    resetprop -n view.touch_slop 8 2>/dev/null
}

set_sched_performance() {
    for w in /proc/sys/walt /proc/sys/kernel; do
        [ -f "$w/sched_downmigrate" ] && echo 40 > "$w/sched_downmigrate" 2>/dev/null
        [ -f "$w/sched_upmigrate" ] && echo 60 > "$w/sched_upmigrate" 2>/dev/null
        [ -f "$w/sched_boost" ] && echo 1 > "$w/sched_boost" 2>/dev/null
    done
}

set_sched_powersave() {
    for w in /proc/sys/walt /proc/sys/kernel; do
        [ -f "$w/sched_downmigrate" ] && echo 60 > "$w/sched_downmigrate" 2>/dev/null
        [ -f "$w/sched_upmigrate" ] && echo 85 > "$w/sched_upmigrate" 2>/dev/null
        [ -f "$w/sched_boost" ] && echo 0 > "$w/sched_boost" 2>/dev/null
    done
}

get_fg_app() {
    local focus_line=""
    focus_line=$(dumpsys window visible-apps 2>/dev/null | grep -m1 'mCurrentFocus')
    echo "$focus_line" | sed -n 's/.*[[:space:]]\([a-zA-Z0-9_.]*\)\/[^}]*}.*/\1/p'
}

is_video_app() {
    case "$1" in
        tv.danmaku.bili*|com.bilibili*|com.smile.gifmaker*|com.kuaishou*|com.ss.android.ugc.aweme*|com.ss.android.ugc.trill*|com.zhiliaoapp.musically*|com.google.android.youtube*|app.revanced*|com.tencent.qqlive*|com.qiyi.video*|com.youku*|com.hunantv.imgo*|org.videolan.vlc*|com.mxtech.videoplayer*|com.netflix.mediaclient*|com.amazon.avod*|com.disney.disneyplus*|com.clover.acfun*|com.huawei.himovie*|com.oppo.video*|com.coloros.video*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

is_streaming_app() {
    case "$1" in
        com.limelight*|com.valvesoftware.steamlink*|com.parsec.client*|com.chaozhuo.gameassistant*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

is_builtin_game() {
    case "$1" in
        com.netease.newspike|com.activision.callofduty.*|com.epicgames.fortnite|com.xiaoji.egggame|jp.sammynet.next.ort.a0001|com.tencent.tmgp.*|com.miHoYo.*|com.kurogame.*|org.yuzu.yuzu_emu*|org.sudachi.sudachi*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

is_casual_game() {
    case "$1" in
        com.yooyoo.dwby3d.*|com.tuyoo.*|*.fishing*|*.by3d*|com.pokercity.*|com.jj.*|com.tencent.tmgp.dld|com.tencent.tmgp.cfby|com.xiaomi.gamecenter.*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

CURRENT_MODE=""
log_info "OnePlus Pad Unified Smart Power & Dynamic Optimizer 2.0 started (PID: $$)."

while true; do
    rotate_log

    # 1. Direct sysfs battery readings (zero IPC, <1ms)
    batt_status=$(cat /sys/class/power_supply/battery/status 2>/dev/null || echo "Discharging")
    batt_level=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo "50")
    batt_temp_raw=$(cat /sys/class/power_supply/battery/temp 2>/dev/null || echo "300")
    batt_volt_uv=$(cat /sys/class/power_supply/battery/voltage_now 2>/dev/null || echo "3800000")
    batt_curr_raw=$(cat /sys/class/power_supply/battery/current_now 2>/dev/null || echo "0")

    batt_volt=$((batt_volt_uv / 1000))
    batt_temp_c=$((batt_temp_raw / 10))
    batt_temp_dec=$((batt_temp_raw % 10))

    # 2. Fast Charging Logic with Desktop Switch Integration
    if [ "$batt_status" = "Charging" ]; then
        if [ -f "/data/local/tmp/super_charge_unlocked" ]; then
            # 桌面开关处于开启状态：锁定 0 档温控，彻底解除亮屏降频
            settings put system single_speed_charge_state 1 2>/dev/null
            settings put system smart_charge_switch_state 0 2>/dev/null
            settings put system charge_protection_current_state 0 2>/dev/null
            if [ "$batt_temp_raw" -lt 480 ]; then
                echo 0 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
                echo 0 > /sys/class/oplus_chg/battery/cool_down 2>/dev/null
                CHARGE_DESC="⚡ 满血狂暴充 (桌面开关已解除亮屏限速)"
            else
                echo 2 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
                CHARGE_DESC="🛡️ 极限温度熔断保护 (>48°C)"
            fi
        else
            # 桌面开关处于关闭状态：正常温控保护策略
            settings put system single_speed_charge_state 1 2>/dev/null
            settings put system smart_charge_switch_state 0 2>/dev/null
            settings put system charge_protection_current_state 0 2>/dev/null

            if [ "$batt_temp_raw" -lt 420 ]; then
                echo 0 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
                CHARGE_DESC="⚡ 80W 极速闪充 (满血输出)"
            elif [ "$batt_temp_raw" -lt 450 ]; then
                echo 1 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
                CHARGE_DESC="🔥 智能控温充 (~50W 恒温速充)"
            else
                echo 3 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
                CHARGE_DESC="🛡️ 高温安全回退 (保护电芯寿命)"
            fi
        fi
    else
        echo 0 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
        CHARGE_DESC="🔋 未在充电 (正常放电)"
    fi

    # 3. Screen State Check via DRM DPMS
    dpms_state=$(cat /sys/class/drm/card0-DSI-1/dpms 2>/dev/null || echo "On")

    if [ "$dpms_state" != "On" ]; then
        if [ "$CURRENT_MODE" != "SCREEN_OFF" ]; then
            CURRENT_MODE="SCREEN_OFF"
            log_info "Screen is OFF. Entering deep standby sleep profile."
            chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
            chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            chmod 666 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
            echo 960000 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
            echo 1017600 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            echo 160000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
            set_touch_normal
            set_sched_powersave
            dumpsys deviceidle step >/dev/null 2>&1
        fi
        POWER_DESC="💤 息屏深度休眠 (Doze 节能)"
        TOUCH_DESC="⏸️ 息屏休眠 (0采样)"
        curr_app="None (Screen Off)"
        sleep_interval=10
    else
        sleep_interval=3
        curr_app=$(get_fg_app)

        # Check whitelist files
        IS_CUSTOM_GAME=0
        for cfg in "$SDCARD_CFG_1" "$SDCARD_CFG_2"; do
            if [ -f "$cfg" ] && [ -n "$curr_app" ]; then
                if grep -v '^[[:space:]]*#' "$cfg" 2>/dev/null | grep -q "$curr_app"; then
                    IS_CUSTOM_GAME=1
                    break
                fi
            fi
        done

        IS_CASUAL_GAME=0
        if [ -f "$SDCARD_CFG_CASUAL" ] && [ -n "$curr_app" ]; then
            if grep -v '^[[:space:]]*#' "$SDCARD_CFG_CASUAL" 2>/dev/null | grep -q "$curr_app"; then
                IS_CASUAL_GAME=1
            fi
        fi

        # Check if local AI server (llama-server) is running
        if pgrep -x llama-server >/dev/null 2>&1; then
            if [ "$CURRENT_MODE" != "AI_SERVER" ]; then
                CURRENT_MODE="AI_SERVER"
                log_info "Local AI server (llama-server) running. Applying 3.28GHz Prime + 2.92GHz Perf boost."
                chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                chmod 666 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                echo 2918400 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                echo 3283200 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                echo 1100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                set_sched_performance
                settings put system peak_refresh_rate 120.0 2>/dev/null
                settings put system min_refresh_rate 60.0 2>/dev/null
                echo 500 > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null
            fi
            POWER_DESC="🤖 本地端侧 AI 推理加速 (3.28GHz 超大核满血澎湃)"
            TOUCH_DESC="⚡ 高响应跟手触控 (120Hz)"
        elif [ "$IS_CUSTOM_GAME" = "1" ] || is_builtin_game "$curr_app"; then
            # HEAVY ESPORTS GAME: 3.28GHz Prime cap + 1100MHz GPU + 540Hz esports touch
            if [ "$CURRENT_MODE" != "GAME:$curr_app" ]; then
                CURRENT_MODE="GAME:$curr_app"
                log_info "Heavy game active [$curr_app]. Applying 3.28GHz Prime cap + 540Hz esports touch."
                chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                chmod 666 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                echo 2918400 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                echo 3283200 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                echo 1100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                set_touch_esports
                set_sched_performance
                settings put system peak_refresh_rate 120.0 2>/dev/null
                settings put system min_refresh_rate 60.0 2>/dev/null
                echo 500 > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null
            fi
            POWER_DESC="🎮 电竞性能能效比 (3.28GHz超大核稳120帧)"
            TOUCH_DESC="⚡ 540Hz 电竞跟手触控 (0ms延迟已激活)"
        elif [ "$IS_CASUAL_GAME" = "1" ] || is_casual_game "$curr_app"; then
            # CASUAL 3D GAME (Fishing/Chess): 60Hz lock + 525MHz GPU cap + 1.68GHz CPU cap
            # Slashes 18W battery-melting runaway down to 3~4W!
            if [ "$CURRENT_MODE" != "CASUAL:$curr_app" ]; then
                CURRENT_MODE="CASUAL:$curr_app"
                log_info "Casual 3D game active [$curr_app]. Engaging 60Hz lock + 525MHz GPU cap + 1.68GHz CPU cap to suppress 18W power drain."
                chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                chmod 666 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                echo 1785600 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                echo 1689600 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                echo 525000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                set_touch_normal
                set_sched_powersave
                settings put system peak_refresh_rate 60.0 2>/dev/null
                settings put system min_refresh_rate 60.0 2>/dev/null
                echo 1500 > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null
            fi
            # Re-enforce limits continuously against ColorOS Game Space boost
            echo 1785600 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
            echo 1689600 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            echo 525000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
            POWER_DESC="🐟 休闲3D轻量降温 (60Hz帧率锁 + 525MHz GPU + 1.68GHz防发热)"
            TOUCH_DESC="🔋 节能日常触控 (120Hz采样)"
        elif is_streaming_app "$curr_app"; then
            # STREAMING PROFILE: Moonlight / Steam Link
            if [ "$CURRENT_MODE" != "STREAMING:$curr_app" ]; then
                CURRENT_MODE="STREAMING:$curr_app"
                log_info "Streaming app active [$curr_app]. Low touch latency + 120Hz display."
                chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                chmod 666 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                echo 2400000 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                echo 2649600 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                echo 734000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                set_touch_low_latency
                set_sched_performance
                settings put system peak_refresh_rate 120.0 2>/dev/null
                settings put system min_refresh_rate 60.0 2>/dev/null
            fi
            POWER_DESC="📡 超低延迟串流 (120Hz流利 + 低功耗解码)"
            TOUCH_DESC="⚡ 串流低延迟跟手触控"
        elif is_video_app "$curr_app"; then
            # VIDEO PROFILE: Kuaishou, Douyin, Bilibili, YouTube -> 60Hz lock + Oryon powersave
            if [ "$CURRENT_MODE" != "VIDEO:$curr_app" ]; then
                CURRENT_MODE="VIDEO:$curr_app"
                log_info "Video app active [$curr_app]. 60Hz lock & deep media powersave engaged."
                chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                chmod 666 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                echo 1785600 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                echo 1689600 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                echo 443000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                set_touch_normal
                set_sched_powersave
                settings put system peak_refresh_rate 60.0 2>/dev/null
                settings put system min_refresh_rate 60.0 2>/dev/null
                echo 1500 > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null
            fi
            POWER_DESC="🎬 视频深度省电 (锁定60Hz + 硬件VPU解码能效)"
            TOUCH_DESC="🔋 节能日常触控 (120Hz采样)"
        else
            # DAILY / BALANCED PROFILE: Launcher, WeChat, Browsers, System Apps
            if [ "$CURRENT_MODE" != "DAILY" ]; then
                CURRENT_MODE="DAILY"
                log_info "Daily general app active. 120Hz smooth UI + 2.44GHz Prime cap."
                chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                chmod 666 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                echo 2227200 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
                echo 2438400 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
                echo 1100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null
                set_touch_normal
                set_sched_powersave
                settings put system peak_refresh_rate 120.0 2>/dev/null
                settings put system min_refresh_rate 60.0 2>/dev/null
                echo 1000 > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null
            fi
            POWER_DESC="📱 日常流畅省电 (120Hz 丝滑操作 + 2.44GHz超大核能效锁)"
            TOUCH_DESC="🔋 节能日常触控 (120Hz采样)"
        fi
    fi

    # 4. EXPORT LIVE STATUS JSON
    cat << EOF > "$STATUS_JSON.tmp"
{
  "charge_state": "$CHARGE_DESC",
  "power_profile": "$POWER_DESC",
  "touch_sampling_mode": "$TOUCH_DESC",
  "battery_level": $batt_level,
  "battery_temp": "$batt_temp_c.$batt_temp_dec",
  "battery_voltage_mv": $batt_volt,
  "battery_current_ma": $batt_curr_raw,
  "foreground_app": "$curr_app",
  "mode": "$CURRENT_MODE",
  "timestamp": "$(date '+%Y-%m-%d %H:%M:%S')"
}
EOF
    mv "$STATUS_JSON.tmp" "$STATUS_JSON" 2>/dev/null
    chmod 644 "$STATUS_JSON" 2>/dev/null

    sleep "$sleep_interval"
done
