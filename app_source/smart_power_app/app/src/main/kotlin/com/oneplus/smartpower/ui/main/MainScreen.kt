package com.oneplus.smartpower.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneplus.smartpower.data.DeviceTelemetry
import com.oneplus.smartpower.data.GameItem
import com.oneplus.smartpower.data.GameMode
import com.oneplus.smartpower.data.InstalledApp
import com.oneplus.smartpower.data.PowerConfig

// --- Material 3 Liquid Glass Palette ---
val DeepSpaceBg = Color(0xFF06090F)
val GlassSurface = Color(0x1A25364E)
val GlassSurfaceHover = Color(0x28384F6E)
val GlassBorder = Brush.linearGradient(
    listOf(Color(0x55FFFFFF), Color(0x15FFFFFF), Color(0x3500E5FF))
)
val GlassBorderSubtle = Brush.linearGradient(
    listOf(Color(0x30FFFFFF), Color(0x0AFFFFFF))
)

val LiquidCyan = Color(0xFF00F0FF)
val LiquidBlue = Color(0xFF2979FF)
val LiquidAmber = Color(0xFFFFAB00)
val LiquidGreen = Color(0xFF00E676)
val LiquidRed = Color(0xFFFF1744)
val LiquidViolet = Color(0xFF7C4DFF)
val GlassTextMuted = Color(0xFF94A3B8)

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    when (val state = uiState) {
        is MainScreenUiState.Ready -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepSpaceBg)
            ) {
                // Liquid Ambient Glows in background
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .offset(x = (-40).dp, y = (-40).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x2500E5FF), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(380.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 100.dp, y = 50.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x1E7C4DFF), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                Scaffold(
                    containerColor = Color.Transparent,
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = { showAddDialog = true },
                            containerColor = LiquidCyan,
                            contentColor = Color.Black,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = LiquidCyan)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("扫描添加应用", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(6.dp)) }

                        // 1. App Header with Glass Badge
                        item {
                            GlassHeaderSection()
                        }

                        // 2. Liquid Glass Telemetry Hero Card
                        item {
                            LiquidTelemetryCard(state.telemetry)
                        }

                        // 3. Liquid Glass Controls Card
                        item {
                            LiquidControlsCard(
                                config = state.config,
                                onToggleFastCharge = { viewModel.toggleFastCharge(it) },
                                onToggleAutoThermal = { viewModel.toggleAutoThermal(it) }
                            )
                        }

                        // 4. Liquid Glass Game Filter Tabs
                        item {
                            LiquidGameFilterSection(
                                selectedFilter = state.selectedFilter,
                                onSelectFilter = { viewModel.setFilter(it) }
                            )
                        }

                        // 5. Liquid Glass Game List
                        val filteredGames = when (state.selectedFilter) {
                            1 -> state.games.filter { it.isCasual }
                            2 -> state.games.filter { !it.isCasual }
                            else -> state.games
                        }

                        if (filteredGames.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(GlassSurface)
                                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("暂无监控应用", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("点击右下角「＋ 扫描添加应用」，自动扫描平板内真实游戏与应用直接开启", color = GlassTextMuted, fontSize = 13.sp)
                                    }
                                }
                            }
                        } else {
                            items(filteredGames, key = { it.packageName }) { game ->
                                LiquidGameItemCard(
                                    game = game,
                                    onToggleType = { viewModel.toggleGameType(game.packageName) },
                                    onDelete = { viewModel.removeGame(game.packageName) }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(70.dp)) }
                    }
                }

                if (showAddDialog) {
                    LaunchedEffect(Unit) {
                        viewModel.scanInstalledApps()
                    }
                    LiquidAppScannerDialog(
                        installedApps = state.installedApps,
                        isScanning = state.isScanning,
                        onDismiss = { showAddDialog = false },
                        onSetAppMode = { pkg, mode ->
                            viewModel.setAppMode(pkg, mode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GlassHeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "OnePlus 极智温控 & 性能工坊",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Material 3 Liquid Glass · Snapdragon 8 Elite 动态温控",
                color = GlassTextMuted,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x2A00E676))
                .border(1.dp, Color(0x5500E676), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(text = "● Root 激活", color = LiquidGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LiquidTelemetryCard(telemetry: DeviceTelemetry) {
    val wattColor by animateColorAsState(
        when {
            telemetry.isCharging -> LiquidGreen
            telemetry.powerWatts > 15.0f -> LiquidRed
            telemetry.powerWatts > 7.0f -> LiquidAmber
            else -> LiquidCyan
        }, label = "wattColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x282C3E55), Color(0x18182230))
                )
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Column {
            // Power Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "实时整机功耗", color = GlassTextMuted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${if (telemetry.isCharging) "+" else "-"}${telemetry.powerWatts}",
                            color = wattColor,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " W",
                            color = wattColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // Liquid Pill Status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x35000000))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "${telemetry.batteryLevel}% · ${if (telemetry.isCharging) "满血闪充" else "电池供电"}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual Liquid Glass Thermals (Battery & CPU)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiquidThermalGauge(
                    title = "电池温度",
                    tempC = telemetry.batteryTempC,
                    isWarn = telemetry.batteryTempC >= 42.0f,
                    isCrit = telemetry.batteryTempC >= 48.0f,
                    modifier = Modifier.weight(1f)
                )
                LiquidThermalGauge(
                    title = "CPU 温度",
                    tempC = telemetry.cpuTempC,
                    isWarn = telemetry.cpuTempC >= 55.0f,
                    isCrit = telemetry.cpuTempC >= 75.0f,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chip Clocks (GPU & CPU) in Frosted Glass Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x250A0F1A))
                    .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LiquidClockItem(label = "GPU 实时频段", value = "${telemetry.gpuFreqMhz} MHz", accent = LiquidCyan)
                LiquidClockItem(label = "CPU 能效小核", value = "${telemetry.cpuLittleFreqMhz} MHz", accent = LiquidAmber)
                LiquidClockItem(label = "CPU 满血超大核", value = "${telemetry.cpuPrimeFreqMhz} MHz", accent = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Mode Banner
            Text(
                text = "⚡ 当前调度: ${telemetry.activeProfileText}",
                color = GlassTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LiquidThermalGauge(
    title: String,
    tempC: Float,
    isWarn: Boolean,
    isCrit: Boolean,
    modifier: Modifier = Modifier
) {
    val tempColor = when {
        isCrit -> LiquidRed
        isWarn -> LiquidAmber
        else -> LiquidGreen
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x250A0F1A))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(text = title, color = GlassTextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${tempC}°C",
                color = tempColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Liquid Gradient Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x301E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (tempC / 60.0f).coerceIn(0.05f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(LiquidCyan, tempColor)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun LiquidClockItem(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = GlassTextMuted, fontSize = 11.sp)
        Text(
            text = value,
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LiquidControlsCard(
    config: PowerConfig,
    onToggleFastCharge: (Boolean) -> Unit,
    onToggleAutoThermal: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Fast Charge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "⚡ 满血狂暴充 (解除亮屏限速)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "屏幕亮起依然维持 80W~100W 满血快充", color = GlassTextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = config.fastChargeEnabled,
                    onCheckedChange = onToggleFastCharge,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = LiquidCyan,
                        uncheckedTrackColor = Color(0x35FFFFFF)
                    )
                )
            }

            HorizontalDivider(color = Color(0x18FFFFFF), thickness = 0.5.dp)

            // Auto Thermal Throttling Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "🛡️ 智能自动温控防过热降频", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "温度触碰 42°C/44°C/48°C 阶梯回退频段，彻底消除 24W 爆热", color = GlassTextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = config.autoThermalThrottle,
                    onCheckedChange = onToggleAutoThermal,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = LiquidGreen,
                        uncheckedTrackColor = Color(0x35FFFFFF)
                    )
                )
            }
        }
    }
}

@Composable
fun LiquidGameFilterSection(
    selectedFilter: Int,
    onSelectFilter: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "游戏调度分类中心", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "点击卡片可即时切换", color = GlassTextMuted, fontSize = 11.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidFilterTab(title = "全部", selected = selectedFilter == 0, onClick = { onSelectFilter(0) }, modifier = Modifier.weight(1f))
            LiquidFilterTab(title = "轻量休闲 (3W防热)", selected = selectedFilter == 1, onClick = { onSelectFilter(1) }, modifier = Modifier.weight(1.3f))
            LiquidFilterTab(title = "重载电竞 (120Hz)", selected = selectedFilter == 2, onClick = { onSelectFilter(2) }, modifier = Modifier.weight(1.3f))
        }
    }
}

@Composable
fun LiquidFilterTab(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) LiquidCyan else GlassSurface)
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.Black else GlassTextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

@Composable
fun LiquidGameItemCard(
    game: GameItem,
    onToggleType: () -> Unit,
    onDelete: () -> Unit
) {
    val isCasual = game.isCasual
    val tagBg = if (isCasual) Color(0x2800E676) else Color(0x28FFAB00)
    val tagBorder = if (isCasual) Color(0x5000E676) else Color(0x50FFAB00)
    val tagColor = if (isCasual) LiquidGreen else LiquidAmber
    val tagText = if (isCasual) "🟢 轻游戏 (525MHz, ~3.5W)" else "⚡ 重载电竞 (120Hz 澎湃)"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
            .clickable { onToggleType() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.appName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = game.packageName,
                    color = GlassTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(tagBg)
                        .border(0.8.dp, tagBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(text = tagText, color = tagColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x35FFFFFF))
                        .clickable { onToggleType() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isCasual) "转为电竞" else "转为轻游戏",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDelete) {
                    Text("✕", fontSize = 16.sp, color = GlassTextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LiquidAppScannerDialog(
    installedApps: List<InstalledApp>,
    isScanning: Boolean,
    onDismiss: () -> Unit,
    onSetAppMode: (pkg: String, mode: GameMode) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 全部, 1: 游戏, 2: 已配置

    val filteredApps = remember(installedApps, searchQuery, selectedTab) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {
                1 -> app.isGame
                2 -> app.mode != GameMode.NONE
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF0101926),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.85f)
            .border(1.2.dp, GlassBorderSubtle, RoundedCornerShape(24.dp)),
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "平板应用 & 游戏调度管理",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = GlassTextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已扫描平板安装的真实应用，点击开关一键分配调度，无需手动输入包名",
                    color = GlassTextMuted,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("🔍 搜索应用或游戏名称...", color = GlassTextMuted, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LiquidCyan,
                        unfocusedBorderColor = Color(0x30FFFFFF),
                        focusedContainerColor = Color(0x20FFFFFF),
                        unfocusedContainerColor = Color(0x10FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("全部应用", "🎮 游戏优先", "已开启配置").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) LiquidCyan else Color(0x18FFFFFF))
                                .clickable { selectedTab = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isScanning) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LiquidCyan)
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("未找到匹配的应用", color = GlassTextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            LiquidAppScannerItem(
                                app = app,
                                onSetMode = { newMode ->
                                    onSetAppMode(app.packageName, newMode)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = LiquidCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "完成", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun LiquidAppScannerItem(
    app: InstalledApp,
    onSetMode: (GameMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x251C2A3A))
            .border(0.8.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (app.isGame) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3500E676))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("🎮 游戏", color = LiquidGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    color = GlassTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Mode Selector: [🟢 轻游戏] [⚡ 电竞]
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Casual
                val isCasual = app.mode == GameMode.CASUAL
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCasual) Color(0x4000E676) else Color(0x18FFFFFF))
                        .border(
                            width = if (isCasual) 1.dp else 0.5.dp,
                            color = if (isCasual) LiquidGreen else Color(0x20FFFFFF),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            if (isCasual) onSetMode(GameMode.NONE) else onSetMode(GameMode.CASUAL)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🟢 轻游戏",
                        color = if (isCasual) LiquidGreen else Color(0xAAFFFFFF),
                        fontSize = 11.sp,
                        fontWeight = if (isCasual) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Heavy
                val isHeavy = app.mode == GameMode.HEAVY
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isHeavy) Color(0x40FFAB00) else Color(0x18FFFFFF))
                        .border(
                            width = if (isHeavy) 1.dp else 0.5.dp,
                            color = if (isHeavy) LiquidAmber else Color(0x20FFFFFF),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            if (isHeavy) onSetMode(GameMode.NONE) else onSetMode(GameMode.HEAVY)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ 电竞",
                        color = if (isHeavy) LiquidAmber else Color(0xAAFFFFFF),
                        fontSize = 11.sp,
                        fontWeight = if (isHeavy) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
