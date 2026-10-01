#!/system/bin/sh
MODDIR=${0%/*}

if [ -f "$MODDIR/sepolicy.rule" ]; then
    magiskpolicy --apply "$MODDIR/sepolicy.rule" --live 2>/dev/null
fi

chmod 666 /sys/devices/virtual/oplus_chg/battery/cool_down 2>/dev/null
chmod 666 /sys/devices/virtual/oplus_chg/battery/normal_cool_down 2>/dev/null
chmod 666 /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null
chmod 666 /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq 2>/dev/null

# Touch nodes permissions
chmod 666 /proc/touchpanel/* 2>/dev/null
