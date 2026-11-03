package com.uvos.settings.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.uvos.settings.SettingsApplication
import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.presentation.contract.SettingsEffect
import com.uvos.settings.presentation.screens.SettingsDashboardScreen
import com.uvos.settings.presentation.screens.profiles.CommunicationProfileScreen
import com.uvos.settings.presentation.screens.profiles.CustomProfileScreen
import com.uvos.settings.presentation.screens.profiles.SterileProfileScreen
import com.uvos.settings.presentation.theme.UvosBackground
import com.uvos.settings.presentation.theme.UvosSettingsTheme
import com.uvos.settings.presentation.viewmodel.SettingsScreen
import com.uvos.settings.presentation.viewmodel.SettingsViewModel

open class SettingsMainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        (application as SettingsApplication).provideViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UvosSettingsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBackground
                ) {
                    SettingsAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SettingsAppNavigation(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Слушатель событий (Toast и Haptic)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowToast -> {
                    // Тост выводится через контекст если требуется
                }
                is SettingsEffect.PlayToggleHaptic -> {
                    // Тактильный отклик
                }
                is SettingsEffect.RequestSystemPermission -> {
                    // Запрос разрешений
                }
            }
        }
    }

    // Обработка кнопки "Назад"
    BackHandler(enabled = currentScreen != SettingsScreen.DASHBOARD) {
        viewModel.navigateTo(SettingsScreen.DASHBOARD)
    }

    when (currentScreen) {
        SettingsScreen.DASHBOARD -> {
            SettingsDashboardScreen(
                state = state,
                onIntent = viewModel::handleIntent,
                onNavigateToProfile = { profile ->
                    when (profile) {
                        SystemProfile.STERILE -> viewModel.navigateTo(SettingsScreen.PROFILE_STERILE)
                        SystemProfile.COMMUNICATION -> viewModel.navigateTo(SettingsScreen.PROFILE_COMMUNICATION)
                        SystemProfile.CUSTOM -> viewModel.navigateTo(SettingsScreen.PROFILE_CUSTOM)
                    }
                }
            )
        }
        SettingsScreen.PROFILE_STERILE -> {
            SterileProfileScreen(
                state = state,
                onIntent = viewModel::handleIntent,
                onBack = { viewModel.navigateTo(SettingsScreen.DASHBOARD) }
            )
        }
        SettingsScreen.PROFILE_COMMUNICATION -> {
            CommunicationProfileScreen(
                state = state,
                onIntent = viewModel::handleIntent,
                onBack = { viewModel.navigateTo(SettingsScreen.DASHBOARD) }
            )
        }
        SettingsScreen.PROFILE_CUSTOM -> {
            CustomProfileScreen(
                state = state,
                onIntent = viewModel::handleIntent,
                onBack = { viewModel.navigateTo(SettingsScreen.DASHBOARD) }
            )
        }
    }
}
