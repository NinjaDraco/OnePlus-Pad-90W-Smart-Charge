#!/system/bin/sh
MODDIR=${0%/*}
LOGFILE="/data/local/tmp/smart_power_opt.log"

mkdir -p /data/local/tmp
echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SERVICE] Waiting for Android boot completion..." >> "$LOGFILE"

while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 2
done

echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SERVICE] Android boot completed. Applying optimizations..." >> "$LOGFILE"

if [ -f "$MODDIR/sepolicy.rule" ]; then
    magiskpolicy --apply "$MODDIR/sepolicy.rule" --live 2>/dev/null
fi

# Ensure user preferred 315 DPI remains permanent
wm density 315 2>/dev/null

# Activate Fast Charging triggers
settings put system single_speed_charge_state 1 2>/dev/null
settings put system smart_charge_switch_state 0 2>/dev/null
settings put system charge_protection_current_state 0 2>/dev/null

# Activate 540Hz Touch Hardware nodes
chmod 666 /proc/touchpanel/* 2>/dev/null
echo 1 > /proc/touchpanel/game_switch_enable 2>/dev/null
echo 1 > /proc/touchpanel/sensitive_level 2>/dev/null
echo 1 > /proc/touchpanel/smooth_level 2>/dev/null
resetprop -n sys.input.resample.latency 0 2>/dev/null
resetprop -n view.touch_slop 2 2>/dev/null

if [ -f "$MODDIR/scripts/smart_power_daemon.sh" ]; then
    (
        while :; do
            if [ ! -f "$MODDIR/scripts/smart_power_daemon.sh" ] || [ -f "$MODDIR/disable" ] || [ -f "$MODDIR/remove" ]; then
                break
            fi
            nohup /system/bin/sh "$MODDIR/scripts/smart_power_daemon.sh" daemon >/dev/null 2>&1 &
            DAEMON_PID=$!
            while kill -0 "$DAEMON_PID" 2>/dev/null; do
                sleep 8
            done
            wait "$DAEMON_PID" 2>/dev/null
            if [ -f "$MODDIR/scripts/smart_power_daemon.sh" ] && [ ! -f "$MODDIR/disable" ] && [ ! -f "$MODDIR/remove" ]; then
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SERVICE] Daemon exited; auto-restarting in 3s..." >> "$LOGFILE"
                sleep 3
            fi
        done
    ) &
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SERVICE] Smart Power & 540Hz Touch Daemon watchdog spawned." >> "$LOGFILE"
fi

exit 0
