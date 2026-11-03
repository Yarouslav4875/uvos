package com.uvos.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.settings.domain.model.AppFilterCategory
import com.uvos.settings.domain.model.AudioStreamType
import com.uvos.settings.domain.model.HapticStrength
import com.uvos.settings.domain.model.SystemFontFamily
import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.domain.repository.SystemSettingsRepository
import com.uvos.settings.domain.usecase.GetAuditedAppsUseCase
import com.uvos.settings.domain.usecase.ObserveTelemetryUseCase
import com.uvos.settings.domain.usecase.SetAppNetworkAccessUseCase
import com.uvos.settings.domain.usecase.SetAudioStreamUseCase
import com.uvos.settings.domain.usecase.SetDisplayParamUseCase
import com.uvos.settings.domain.usecase.SwitchProfileUseCase
import com.uvos.settings.domain.usecase.TerminateAppUseCase
import com.uvos.settings.domain.usecase.ToggleAppIsolationUseCase
import com.uvos.settings.presentation.contract.SettingsEffect
import com.uvos.settings.presentation.contract.SettingsIntent
import com.uvos.settings.presentation.contract.SettingsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsScreen {
    DASHBOARD,
    PROFILE_STERILE,
    PROFILE_COMMUNICATION,
    PROFILE_CUSTOM
}

class SettingsViewModel(
    private val switchProfileUseCase: SwitchProfileUseCase,
    private val observeTelemetryUseCase: ObserveTelemetryUseCase,
    private val getAuditedAppsUseCase: GetAuditedAppsUseCase,
    private val setDisplayParamUseCase: SetDisplayParamUseCase,
    private val setAudioStreamUseCase: SetAudioStreamUseCase,
    private val toggleAppIsolationUseCase: ToggleAppIsolationUseCase,
    private val setAppNetworkAccessUseCase: SetAppNetworkAccessUseCase,
    private val terminateAppUseCase: TerminateAppUseCase,
    private val systemSettingsRepository: SystemSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsState(
            screenBrightness = systemSettingsRepository.getBrightness(),
            isAutoBrightness = systemSettingsRepository.isAutoBrightness(),
            refreshRateHz = systemSettingsRepository.getRefreshRate(),
            screenTimeoutSeconds = systemSettingsRepository.getScreenTimeout(),
            volumeRing = systemSettingsRepository.getStreamVolume(AudioStreamType.RING),
            volumeMedia = systemSettingsRepository.getStreamVolume(AudioStreamType.MEDIA),
            volumeAlarm = systemSettingsRepository.getStreamVolume(AudioStreamType.ALARM),
            activeFont = systemSettingsRepository.getActiveFont()
        )
    )
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    private val _currentScreen = MutableStateFlow(SettingsScreen.DASHBOARD)
    val currentScreen: StateFlow<SettingsScreen> = _currentScreen.asStateFlow()

    private val _effectChannel = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effectChannel.receiveAsFlow()

    init {
        startTelemetryObservation()
        observeAuditedApps()
        refreshApps()
    }

    fun navigateTo(screen: SettingsScreen) {
        _currentScreen.value = screen
    }

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SelectProfile -> selectProfile(intent.profile)
            is SettingsIntent.ChangeBrightness -> changeBrightness(intent.value)
            is SettingsIntent.ToggleAutoBrightness -> toggleAutoBrightness(intent.enabled)
            is SettingsIntent.ChangeRefreshRate -> changeRefreshRate(intent.rateHz)
            is SettingsIntent.ChangeScreenTimeout -> changeScreenTimeout(intent.seconds)
            is SettingsIntent.ChangeVolume -> changeVolume(intent.streamType, intent.level)
            is SettingsIntent.SelectFont -> selectFont(intent.font)
            is SettingsIntent.FilterApps -> filterApps(intent.category)
            is SettingsIntent.ToggleAppIsolation -> toggleAppIsolation(intent.packageName, intent.isolate)
            is SettingsIntent.SetAppNetworkAccess -> setAppNetworkAccess(intent.packageName, intent.allowNetwork)
            is SettingsIntent.TerminateApp -> terminateApp(intent.packageName)
            is SettingsIntent.RefreshAppsList -> refreshApps()
        }
    }

    private fun selectProfile(profile: SystemProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileSwitching = true) }
            _effectChannel.send(SettingsEffect.PlayToggleHaptic(HapticStrength.HEAVY))

            val result = switchProfileUseCase(profile)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        currentProfile = profile,
                        isProfileSwitching = false,
                        errorMessage = null
                    )
                }
                _effectChannel.send(SettingsEffect.ShowToast("Профиль [${profile.displayName}] активирован"))
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProfileSwitching = false,
                        errorMessage = err.message ?: "Ошибка переключения профиля"
                    )
                }
                _effectChannel.send(SettingsEffect.ShowToast("Сбой переключения профиля: ${err.message}"))
            }
        }
    }

    private fun startTelemetryObservation() {
        observeTelemetryUseCase(pollIntervalMs = 1000L)
            .onEach { snapshot ->
                _uiState.update { state ->
                    state.copy(
                        batteryVoltageMv = snapshot.batteryVoltageMv,
                        batteryCurrentMa = snapshot.batteryCurrentMa,
                        batteryTemperatureC = snapshot.batteryTempC,
                        socTemperatureC = snapshot.socTempC,
                        cpuTotalLoadPercent = snapshot.totalCpuUsagePercent,
                        coreFrequenciesMhz = snapshot.cores.map { it.frequencyMhz }
                    )
                }
            }
            .catch { /* fallback silently */ }
            .launchIn(viewModelScope)
    }

    private fun observeAuditedApps() {
        getAuditedAppsUseCase(_uiState.value.appFilter)
            .onEach { apps ->
                _uiState.update { it.copy(installedApps = apps, isAuditScanning = false) }
            }
            .catch { /* fallback */ }
            .launchIn(viewModelScope)
    }

    private fun refreshApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuditScanning = true) }
            getAuditedAppsUseCase.refresh()
            _uiState.update { it.copy(isAuditScanning = false) }
        }
    }

    private fun filterApps(category: AppFilterCategory) {
        _uiState.update { it.copy(appFilter = category) }
        observeAuditedApps()
    }

    private fun toggleAppIsolation(packageName: String, isolate: Boolean) {
        viewModelScope.launch {
            _effectChannel.send(SettingsEffect.PlayToggleHaptic(HapticStrength.MEDIUM))
            val result = toggleAppIsolationUseCase(packageName, isolate)
            result.onSuccess {
                val action = if (isolate) "изолировано" else "разблокировано"
                _effectChannel.send(SettingsEffect.ShowToast("Приложение $packageName $action"))
            }.onFailure { err ->
                _effectChannel.send(SettingsEffect.ShowToast("Ошибка изоляции: ${err.message}"))
            }
        }
    }

    private fun setAppNetworkAccess(packageName: String, allowNetwork: Boolean) {
        viewModelScope.launch {
            _effectChannel.send(SettingsEffect.PlayToggleHaptic(HapticStrength.LIGHT))
            val result = setAppNetworkAccessUseCase(packageName, allowNetwork)
            result.onSuccess {
                val state = if (allowNetwork) "разрешен" else "заблокирован"
                _effectChannel.send(SettingsEffect.ShowToast("Доступ в сеть для $packageName $state"))
            }.onFailure { err ->
                _effectChannel.send(SettingsEffect.ShowToast("Ошибка настройки сети: ${err.message}"))
            }
        }
    }

    private fun terminateApp(packageName: String) {
        viewModelScope.launch {
            _effectChannel.send(SettingsEffect.PlayToggleHaptic(HapticStrength.HEAVY))
            val result = terminateAppUseCase(packageName)
            result.onSuccess {
                _effectChannel.send(SettingsEffect.ShowToast("Процесс $packageName остановлен"))
            }.onFailure { err ->
                _effectChannel.send(SettingsEffect.ShowToast("Ошибка остановки: ${err.message}"))
            }
        }
    }

    private fun changeBrightness(value: Int) {
        setDisplayParamUseCase.setBrightness(value)
        _uiState.update { it.copy(screenBrightness = value, isAutoBrightness = false) }
    }

    private fun toggleAutoBrightness(enabled: Boolean) {
        setDisplayParamUseCase.setAutoBrightness(enabled)
        _uiState.update { it.copy(isAutoBrightness = enabled) }
    }

    private fun changeRefreshRate(rateHz: Int) {
        setDisplayParamUseCase.setRefreshRate(rateHz)
        _uiState.update { it.copy(refreshRateHz = rateHz) }
    }

    private fun changeScreenTimeout(seconds: Int) {
        setDisplayParamUseCase.setScreenTimeout(seconds)
        _uiState.update { it.copy(screenTimeoutSeconds = seconds) }
    }

    private fun changeVolume(streamType: AudioStreamType, level: Int) {
        setAudioStreamUseCase(streamType, level)
        _uiState.update { state ->
            when (streamType) {
                AudioStreamType.RING -> state.copy(volumeRing = level)
                AudioStreamType.MEDIA -> state.copy(volumeMedia = level)
                AudioStreamType.ALARM -> state.copy(volumeAlarm = level)
            }
        }
    }

    private fun selectFont(font: SystemFontFamily) {
        systemSettingsRepository.setActiveFont(font)
        _uiState.update { it.copy(activeFont = font) }
        viewModelScope.launch {
            _effectChannel.send(SettingsEffect.ShowToast("Установлен системный шрифт: ${font.displayName}"))
        }
    }
}
