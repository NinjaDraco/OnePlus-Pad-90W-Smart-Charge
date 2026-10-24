#!/system/bin/sh
FLAG_FILE="/data/local/tmp/super_charge_unlocked"

if [ -f "$FLAG_FILE" ]; then
    rm -f "$FLAG_FILE"
    settings put system smart_charge_switch_state 1 2>/dev/null
    settings put system charge_protection_current_state 1 2>/dev/null
    echo "🛡️【智能温控已恢复】已关闭满血快充，恢复默认保护策略"
else
    touch "$FLAG_FILE"
    echo 0 > /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
    echo 0 > /sys/class/oplus_chg/battery/cool_down 2>/dev/null
    settings put system single_speed_charge_state 1 2>/dev/null
    settings put system smart_charge_switch_state 0 2>/dev/null
    settings put system charge_protection_current_state 0 2>/dev/null
    echo "⚡【满血快充已开启】已解除亮屏与温控限制！"
fi
