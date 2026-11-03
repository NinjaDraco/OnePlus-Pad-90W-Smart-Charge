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

# Systemless bind-mount refresh_rate_config.xml to unlock native 120Hz for Fortnite & high-refresh games
if [ -f "$MODDIR/refresh_rate_config.xml" ]; then
    chcon u:object_r:system_file:s0 "$MODDIR/refresh_rate_config.xml" 2>/dev/null
    mount -o bind "$MODDIR/refresh_rate_config.xml" /my_product/etc/refresh_rate_config.xml 2>/dev/null
fi

# Systemless bind-mount sys_thermal_control_config.xml (Anti-Throttle & 120Hz lock under charging)
if [ -f "$MODDIR/sys_thermal_control_config.xml" ]; then
    chcon u:object_r:vendor_configs_file:s0 "$MODDIR/sys_thermal_control_config.xml" 2>/dev/null
    mount -o bind "$MODDIR/sys_thermal_control_config.xml" /odm/etc/temperature_profile/sys_thermal_control_config.xml 2>/dev/null
fi
