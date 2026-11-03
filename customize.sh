ui_print "********************************************************"
ui_print "* OnePlus Pad Pro 智能极速充 + 桌面开关 + 540Hz 触控   *"
ui_print "*    快充拉满 (90W) + 桌面一键开关 + 视频日常极低功耗 *"
ui_print "********************************************************"
ui_print "- 正在配置模块执行权限与守护进程..."
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/service.sh 0 0 0755
set_perm $MODPATH/post-fs-data.sh 0 0 0755
set_perm_recursive $MODPATH/scripts 0 0 0755 0755
ui_print "- 正在注入 SELinux 充电与触控权限规则..."
if [ -f "$MODPATH/sepolicy.rule" ]; then
    magiskpolicy --apply "$MODPATH/sepolicy.rule" --live 2>/dev/null
fi
if [ -f "$MODPATH/refresh_rate_config.xml" ]; then
    ui_print "- 正在注入 120Hz 高刷配置 (解锁 Fortnite 等 90Hz/ViewOverride 锁帧)..."
    chcon u:object_r:system_file:s0 "$MODPATH/refresh_rate_config.xml" 2>/dev/null
    mount -o bind "$MODPATH/refresh_rate_config.xml" /my_product/etc/refresh_rate_config.xml 2>/dev/null
    cp -f "$MODPATH/refresh_rate_config.xml" /data/system/refresh_rate_config.xml 2>/dev/null
    chown system:system /data/system/refresh_rate_config.xml 2>/dev/null
    chcon u:object_r:system_data_file:s0 /data/system/refresh_rate_config.xml 2>/dev/null
fi
if [ -f "$MODPATH/sys_thermal_control_config.xml" ]; then
    ui_print "- 正在注入防降频温控表 (解除边充边玩 60Hz 与 1152MHz 锁帧)..."
    chcon u:object_r:vendor_configs_file:s0 "$MODPATH/sys_thermal_control_config.xml" 2>/dev/null
    mount -o bind "$MODPATH/sys_thermal_control_config.xml" /odm/etc/temperature_profile/sys_thermal_control_config.xml 2>/dev/null
fi
ui_print "- 正在部署独立快充开关脚本..."
if [ -f "$MODPATH/scripts/toggle_fast_charge.sh" ]; then
    cp "$MODPATH/scripts/toggle_fast_charge.sh" /data/local/tmp/toggle_fast_charge.sh 2>/dev/null
    chmod 777 /data/local/tmp/toggle_fast_charge.sh 2>/dev/null
fi
if [ -f "$MODPATH/casual_powersave_games.txt" ] && [ ! -f /sdcard/casual_powersave_games.txt ]; then
    cp "$MODPATH/casual_powersave_games.txt" /sdcard/casual_powersave_games.txt 2>/dev/null
fi
ui_print "- 正在激活系统极速闪充与 540Hz 电竞线性触控..."
settings put system single_speed_charge_state 1 2>/dev/null
settings put system smart_charge_switch_state 0 2>/dev/null
resetprop -n sys.input.resample.latency -1 2>/dev/null
resetprop -n persist.vendor.qti.input.resampling 0 2>/dev/null
ui_print "- 模块安装完成！重启后自动常驻守护，搭配 SmartPowerOpt 极智温控工坊即可全面监控功耗与游戏调度。"
