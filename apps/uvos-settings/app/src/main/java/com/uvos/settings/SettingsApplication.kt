package com.uvos.settings

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.SensorPrivacyManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.topjohnwu.superuser.Shell
import com.uvos.settings.data.datasource.AppAuditDataSource
import com.uvos.settings.data.datasource.RadioControllerDataSource
import com.uvos.settings.data.datasource.SettingsProviderDataSource
import com.uvos.settings.data.datasource.SysfsTelemetryDataSource
import com.uvos.settings.data.repository.AppAuditRepositoryImpl
import com.uvos.settings.data.repository.HardwareTelemetryRepositoryImpl
import com.uvos.settings.data.repository.SystemProfileRepositoryImpl
import com.uvos.settings.data.repository.SystemSettingsRepositoryImpl
import com.uvos.settings.domain.repository.AppAuditRepository
import com.uvos.settings.domain.repository.HardwareTelemetryRepository
import com.uvos.settings.domain.repository.ProfileRepository
import com.uvos.settings.domain.repository.SystemSettingsRepository
import com.uvos.settings.domain.usecase.GetAuditedAppsUseCase
import com.uvos.settings.domain.usecase.ObserveTelemetryUseCase
import com.uvos.settings.domain.usecase.SetAppNetworkAccessUseCase
import com.uvos.settings.domain.usecase.SetAudioStreamUseCase
import com.uvos.settings.domain.usecase.SetDisplayParamUseCase
import com.uvos.settings.domain.usecase.SwitchProfileUseCase
import com.uvos.settings.domain.usecase.TerminateAppUseCase
import com.uvos.settings.domain.usecase.ToggleAppIsolationUseCase
import com.uvos.settings.presentation.viewmodel.SettingsViewModel

class SettingsApplication : Application() {

    lateinit var sysfsTelemetryDataSource: SysfsTelemetryDataSource
        private set
    lateinit var settingsProviderDataSource: SettingsProviderDataSource
        private set
    lateinit var radioControllerDataSource: RadioControllerDataSource
        private set
    lateinit var appAuditDataSource: AppAuditDataSource
        private set

    lateinit var hardwareTelemetryRepository: HardwareTelemetryRepository
        private set
    lateinit var systemSettingsRepository: SystemSettingsRepository
        private set
    lateinit var systemProfileRepository: ProfileRepository
        private set
    lateinit var appAuditRepository: AppAuditRepository
        private set

    lateinit var observeTelemetryUseCase: ObserveTelemetryUseCase
        private set
    lateinit var switchProfileUseCase: SwitchProfileUseCase
        private set
    lateinit var getAuditedAppsUseCase: GetAuditedAppsUseCase
        private set
    lateinit var setDisplayParamUseCase: SetDisplayParamUseCase
        private set
    lateinit var setAudioStreamUseCase: SetAudioStreamUseCase
        private set
    lateinit var toggleAppIsolationUseCase: ToggleAppIsolationUseCase
        private set
    lateinit var setAppNetworkAccessUseCase: SetAppNetworkAccessUseCase
        private set
    lateinit var terminateAppUseCase: TerminateAppUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Инициализация Libsu Root Shell
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val sensorPrivacyManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(SensorPrivacyManager::class.java)
        } else {
            null
        }

        // DataSources
        sysfsTelemetryDataSource = SysfsTelemetryDataSource()
        settingsProviderDataSource = SettingsProviderDataSource(contentResolver, audioManager)
        radioControllerDataSource = RadioControllerDataSource(
            context = this,
            wifiManager = wifiManager,
            bluetoothAdapter = bluetoothAdapter,
            nfcAdapter = nfcAdapter,
            sensorPrivacyManager = sensorPrivacyManager
        )
        appAuditDataSource = AppAuditDataSource(this)

        // Repositories
        hardwareTelemetryRepository = HardwareTelemetryRepositoryImpl(sysfsTelemetryDataSource)
        systemSettingsRepository = SystemSettingsRepositoryImpl(settingsProviderDataSource)
        systemProfileRepository = SystemProfileRepositoryImpl(radioControllerDataSource)
        appAuditRepository = AppAuditRepositoryImpl(appAuditDataSource)

        // UseCases
        observeTelemetryUseCase = ObserveTelemetryUseCase(hardwareTelemetryRepository)
        switchProfileUseCase = SwitchProfileUseCase(systemProfileRepository)
        getAuditedAppsUseCase = GetAuditedAppsUseCase(appAuditRepository)
        setDisplayParamUseCase = SetDisplayParamUseCase(systemSettingsRepository)
        setAudioStreamUseCase = SetAudioStreamUseCase(systemSettingsRepository)
        toggleAppIsolationUseCase = ToggleAppIsolationUseCase(appAuditRepository)
        setAppNetworkAccessUseCase = SetAppNetworkAccessUseCase(appAuditRepository)
        terminateAppUseCase = TerminateAppUseCase(appAuditRepository)
    }

    fun provideViewModelFactory(): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    return SettingsViewModel(
                        switchProfileUseCase = switchProfileUseCase,
                        observeTelemetryUseCase = observeTelemetryUseCase,
                        getAuditedAppsUseCase = getAuditedAppsUseCase,
                        setDisplayParamUseCase = setDisplayParamUseCase,
                        setAudioStreamUseCase = setAudioStreamUseCase,
                        toggleAppIsolationUseCase = toggleAppIsolationUseCase,
                        setAppNetworkAccessUseCase = setAppNetworkAccessUseCase,
                        terminateAppUseCase = terminateAppUseCase,
                        systemSettingsRepository = systemSettingsRepository
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    companion object {
        lateinit var instance: SettingsApplication
            private set
    }
}
