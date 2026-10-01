ui_print "********************************************************"
ui_print "* OnePlus Pad Pro (SM8750) 智能 90W 极速充 + 540Hz 触控 *"
ui_print "*    快充拉满 (90W) + 540Hz 电竞跟手 + 视频日常极低功耗 *"
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
ui_print "- 正在激活系统极速闪充与 540Hz 电竞触控采样..."
settings put system single_speed_charge_state 1 2>/dev/null
settings put system smart_charge_switch_state 0 2>/dev/null
resetprop -n sys.input.resample.latency 0 2>/dev/null
ui_print "- 模块安装完成！重启后自动常驻守护，插电即拉满 90W，触控超频 540Hz，看视频极低功耗！"
