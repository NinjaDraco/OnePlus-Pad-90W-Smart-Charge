#!/system/bin/sh
##########################################################################################
# OnePlus Pad Pro - Smart 90W Charge, Power Efficiency & 540Hz Touch Daemon
# Platform: Snapdragon 8 Elite (SM8750) / OxygenOS 16 / Android 16
##########################################################################################

LOG_FILE="/data/local/tmp/smart_power_opt.log"
STATUS_JSON="/data/local/tmp/smart_power_status.json"

log_info() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] [SMART_POWER] $1"
    echo "$msg" >> "$LOG_FILE" 2>/dev/null
}

# Keep log file size bounded (< 256KB)
rotate_log() {
    if [ -f "$LOG_FILE" ]; then
        local size=$(wc -c < "$LOG_FILE" 2>/dev/null || echo 0)
        if [ "$size" -gt 262144 ] 2>/dev/null; then
            tail -n 400 "$LOG_FILE" > "$LOG_FILE.tmp" 2>/dev/null
            mv "$LOG_FILE.tmp" "$LOG_FILE" 2>/dev/null
        fi
    fi
}

get_fg_app() {
    local app=""
    app=$(dumpsys window 2>/dev/null | sed -n 's/.*mFocusedApp=.* \([a-zA-Z0-9_.]*\)\/[^}]*}.*/\1/p' | head -n 1)
    if [ -z "$app" ]; then
        app=$(dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | grep -v 'NotificationShade' | sed -n 's/.* \([a-zA-Z0-9_.]*\)\/[^}]*}.*/\1/p' | head -n 1)
    fi
    echo "$app"
}

is_video_app() {
    case "$1" in
        tv.danmaku.bili*|com.bilibili*|com.google.android.youtube*|app.revanced*|com.tencent.qqlive*|com.qiyi.video*|com.youku*|com.ss.android.ugc.aweme*|com.zhiliaoapp.musically*|org.videolan.vlc*|com.mxtech.videoplayer*|com.netflix.mediaclient*|com.hunantv.imgo.activity*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

is_game_app() {
    case "$1" in
        com.epicgames.fortnite|com.netease.newspike|com.activision.callofduty.shooter|com.xiaoji.egggame|jp.sammynet.next.ort.a0001|com.tencent.tmgp.pubgmhd|com.tencent.tmgp.sgame|com.miHoYo.*)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

enable_540hz_touch() {
    chmod 666 /proc/touchpanel/* 2>/dev/null
    echo 1 > /proc/touchpanel/game_switch_enable 2>/dev/null
    echo 1 > /proc/touchpanel/sensitive_level 2>/dev/null
    echo 1 > /proc/touchpanel/smooth_level 2>/dev/null
    resetprop -n sys.input.resample.latency 0 2>/dev/null
    resetprop -n persist.vendor.qti.input.resampling 1 2>/dev/null
    resetprop -n view.touch_slop 2 2>/dev/null
}

restore_normal_touch() {
    chmod 666 /proc/touchpanel/* 2>/dev/null
    echo 0 > /proc/touchpanel/game_switch_enable 2>/dev/null
    echo 0 > /proc/touchpanel/sensitive_level 2>/dev/null
    echo 0 > /proc/touchpanel/smooth_level 2>/dev/null
    resetprop -n sys.input.resample.latency 5 2>/dev/null
    resetprop -n view.touch_slop 8 2>/dev/null
}

log_info "Starting OnePlus Pad Pro Smart Power & 540Hz Touch Daemon..."

CURRENT_MODE="INIT"

# Initial touch config: 540Hz enabled
enable_540hz_touch

while true; do
    rotate_log

    # 1. READ BATTERY & CHARGING METRICS
    batt_info=$(dumpsys battery 2>/dev/null)
    chg_status=$(echo "$batt_info" | sed -n 's/^[[:space:]]*status:[[:space:]]*//p' | head -n 1)
    batt_level=$(echo "$batt_info" | sed -n 's/^[[:space:]]*level:[[:space:]]*//p' | head -n 1)
    batt_temp_raw=$(echo "$batt_info" | sed -n 's/^[[:space:]]*temperature:[[:space:]]*//p' | head -n 1)
    batt_volt=$(echo "$batt_info" | sed -n 's/^[[:space:]]*voltage:[[:space:]]*//p' | head -n 1)
    batt_curr=$(echo "$batt_info" | sed -n 's/^[[:space:]]*Battery current :[[:space:]]*//p' | head -n 1)
    [ -z "$batt_curr" ] && batt_curr=0
    [ -z "$batt_temp_raw" ] && batt_temp_raw=300
    [ -z "$batt_level" ] && batt_level=50
    [ -z "$batt_volt" ] && batt_volt=4000

    batt_temp_c=$((batt_temp_raw / 10))
    batt_temp_dec=$((batt_temp_raw % 10))

    # 2. SMART 90W FAST CHARGING LOGIC
    if [ "$chg_status" = "2" ]; then
        settings put system single_speed_charge_state 1 2>/dev/null
        settings put system smart_charge_switch_state 0 2>/dev/null
        settings put system charge_protection_current_state 0 2>/dev/null

        if [ "$batt_temp_raw" -lt 420 ]; then
            echo 0 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
            CHARGE_DESC="⚡ 90W 极速闪充 (满血输出)"
        elif [ "$batt_temp_raw" -lt 450 ]; then
            echo 1 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
            CHARGE_DESC="🔥 智能控温充 (~55W 恒温速充)"
        else
            echo 3 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
            CHARGE_DESC="🛡️ 高温安全回退 (保护电芯寿命)"
        fi
    else
        CHARGE_DESC="🔋 未在充电 (正常放电)"
    fi

    # 3. FOREGROUND APP & POWER EFFICIENCY PROFILE
    curr_app=$(get_fg_app)
    
    # Check screen state
    is_screen_off=$(dumpsys display 2>/dev/null | grep -m1 'mState=' | grep -o 'OFF')
    if [ -n "$is_screen_off" ]; then
        if [ "$CURRENT_MODE" != "SCREEN_OFF" ]; then
            CURRENT_MODE="SCREEN_OFF"
            log_info "Screen turned off. Entering deep standby profile."
            chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            echo 4320000 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            restore_normal_touch
            dumpsys deviceidle step 2>/dev/null
        fi
        POWER_DESC="💤 息屏深度休眠 (Doze)"
        TOUCH_DESC="⏸️ 息屏休眠"
    elif is_video_app "$curr_app"; then
        if [ "$CURRENT_MODE" != "VIDEO" ]; then
            CURRENT_MODE="VIDEO"
            log_info "Video app detected ($curr_app). Switching to 60Hz and Oryon Power-Saver profile."
            settings put system peak_refresh_rate 60.0 2>/dev/null
            settings put system min_refresh_rate 60.0 2>/dev/null
            chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            echo 2246400 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            echo 1500 > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null
            restore_normal_touch
        fi
        POWER_DESC="🎬 视频超低能耗 (60Hz自适应 + Oryon能效锁)"
        TOUCH_DESC="🔋 节能日常触控 (120Hz 采样)"
    elif is_game_app "$curr_app"; then
        if [ "$CURRENT_MODE" != "GAME" ]; then
            CURRENT_MODE="GAME"
            log_info "Game app detected ($curr_app). Activating 540Hz Ultra Touch & Performance."
            enable_540hz_touch
        fi
        POWER_DESC="🎮 极速游戏满血 (Game Mode 优先接管)"
        TOUCH_DESC="⚡ 540Hz 电竞跟手触控 (0ms延迟已激活)"
    else
        # Daily desktop / browsing / general apps
        if [ "$CURRENT_MODE" != "DAILY" ]; then
            CURRENT_MODE="DAILY"
            log_info "Switched to Daily Balanced profile (120Hz, dynamic Oryon scaling, 540Hz responsive touch)."
            settings put system peak_refresh_rate 120.0 2>/dev/null
            settings put system min_refresh_rate 60.0 2>/dev/null
            chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            echo 4320000 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null
            echo 500 > /proc/sys/vm/dirty_writeback_centisecs 2>/dev/null
            # Enable 540Hz touch in daily usage for super crisp UI flicks
            enable_540hz_touch
        fi
        POWER_DESC="📱 日常流畅均衡 (120Hz 动态流利)"
        TOUCH_DESC="⚡ 540Hz 极速跟手触控 (已激活)"
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
  "battery_current_ma": $batt_curr,
  "foreground_app": "$curr_app",
  "mode": "$CURRENT_MODE",
  "timestamp": "$(date '+%Y-%m-%d %H:%M:%S')"
}
EOF
    mv "$STATUS_JSON.tmp" "$STATUS_JSON" 2>/dev/null
    chmod 644 "$STATUS_JSON" 2>/dev/null

    sleep 4
done
