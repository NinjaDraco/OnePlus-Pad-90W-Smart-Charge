package com.oneplus.smartpower.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneplus.smartpower.data.DeviceTelemetry
import com.oneplus.smartpower.data.GameItem
import com.oneplus.smartpower.data.GameMode
import com.oneplus.smartpower.data.InstalledApp
import com.oneplus.smartpower.data.PowerConfig
import com.oneplus.smartpower.data.PowerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MainScreenUiState {
    data class Ready(
        val telemetry: DeviceTelemetry,
        val games: List<GameItem>,
        val installedApps: List<InstalledApp> = emptyList(),
        val config: PowerConfig,
        val selectedFilter: Int = 0, // 0: 全部, 1: 轻量休闲, 2: 重载电竞
        val isScanning: Boolean = false
    ) : MainScreenUiState
}

class MainScreenViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: PowerRepository = PowerRepository()

    private val context = application.applicationContext
    private val _config = MutableStateFlow(PowerConfig())
    private val _games = MutableStateFlow<List<GameItem>>(emptyList())
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _filter = MutableStateFlow(0)
    private val _telemetry = MutableStateFlow(DeviceTelemetry())
    private val _isScanning = MutableStateFlow(false)

    private val _uiState = MutableStateFlow<MainScreenUiState>(
        MainScreenUiState.Ready(
            telemetry = _telemetry.value,
            games = _games.value,
            installedApps = _installedApps.value,
            config = _config.value,
            selectedFilter = 0,
            isScanning = false
        )
    )
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    init {
        loadConfiguredGames()
        observeTelemetry()
    }

    private fun observeTelemetry() {
        viewModelScope.launch {
            repository.telemetryFlow.collect { tel ->
                _telemetry.value = tel
                repository.applyDynamicThermal(tel, _config.value)
                updateState()
            }
        }
    }

    fun loadConfiguredGames() {
        viewModelScope.launch(Dispatchers.IO) {
            val games = repository.loadConfiguredGames(context)
            _games.value = games
            updateState()
        }
    }

    fun scanInstalledApps() {
        _isScanning.value = true
        updateState()
        viewModelScope.launch(Dispatchers.IO) {
            val apps = repository.scanInstalledApps(context)
            _installedApps.value = apps
            _isScanning.value = false
            updateState()
        }
    }

    fun setAppMode(pkg: String, mode: GameMode) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAppMode(pkg, mode)
            val updatedGames = repository.loadConfiguredGames(context)
            val updatedApps = repository.scanInstalledApps(context)
            _games.value = updatedGames
            _installedApps.value = updatedApps
            updateState()
        }
    }

    private fun updateState() {
        _uiState.value = MainScreenUiState.Ready(
            telemetry = _telemetry.value,
            games = _games.value,
            installedApps = _installedApps.value,
            config = _config.value,
            selectedFilter = _filter.value,
            isScanning = _isScanning.value
        )
    }

    fun setFilter(filterIndex: Int) {
        _filter.value = filterIndex
        updateState()
    }

    fun toggleFastCharge(enabled: Boolean) {
        _config.value = _config.value.copy(fastChargeEnabled = enabled)
        viewModelScope.launch(Dispatchers.IO) {
            repository.setFastCharge(enabled)
            updateState()
        }
    }

    fun toggleAutoThermal(enabled: Boolean) {
        _config.value = _config.value.copy(autoThermalThrottle = enabled)
        updateState()
    }

    fun toggleGameType(pkg: String) {
        val updated = _games.value.map {
            if (it.packageName == pkg) it.copy(isCasual = !it.isCasual) else it
        }
        _games.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveGameList(updated)
            val refreshed = repository.loadConfiguredGames(context)
            _games.value = refreshed
            updateState()
        }
    }

    fun removeGame(pkg: String) {
        setAppMode(pkg, GameMode.NONE)
    }
}
