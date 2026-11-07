package com.uvos.compass.feature.compass

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.compass.core.sensors.CompassSensorReader
import com.uvos.compass.domain.model.CompassUiState
import com.uvos.compass.domain.model.OrientationSample
import com.uvos.compass.domain.model.SensorQualifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Instrument ViewModel.
 *
 * - owns the sensor reader and its registration lifecycle via [start]/[stop]
 *   (called from a lifecycle-aware effect on the UI side);
 * - low-pass filters the incoming course and bank values so the needle never
 *   twitches, then pushes everything through a conflated StateFlow.
 */
class CompassViewModel(application: Application) : AndroidViewModel(application) {

    private val reader = CompassSensorReader(
        application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    )

    private val _uiState = MutableStateFlow(
        CompassUiState(
            qualifier = if (reader.hasAnySensor) SensorQualifier.OK else SensorQualifier.MISSING
        )
    )
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    private var smoothHeading = 0f
    private var smoothBank = 0f
    private var startedCollecting = false

    init {
        // Conflated channel: only the freshest orientation sample is retained,
        // no backpressure needed for an instrument moving once per frame.
        if (!startedCollecting) {
            startedCollecting = true
            viewModelScope.launch {
                reader.samples.collect { sample ->
                    if (sample != null) onSample(sample)
                }
            }
        }
    }

    /** Register sensor listeners. Idempotent. */
    fun start() {
        if (reader.hasAnySensor) reader.start()
    }

    /** Unregister sensor listeners. Idempotent. */
    fun stop() {
        reader.stop()
    }

    private fun onSample(sample: OrientationSample) {
        val qualifier = reader.qualifierForAccuracy(sample.accuracy)

        // Shortest-path exponential damping; no jumpy unwrapping artifacts.
        smoothHeading = smoothAngle(smoothHeading, sample.headingDeg, HEADING_ALPHA)
        smoothBank = smoothAngle(smoothBank, sample.rollDeg, BANK_ALPHA)

        // Snap-shortcut: if the raw reading already sits close to the smoothed
        // value, avoid a frozen needle on slow processing forks.
        if (abs(shortestDelta(smoothHeading, sample.headingDeg)) < 0.05f) {
            smoothHeading = sample.headingDeg
        }

        _uiState.value = CompassUiState(
            qualifier = qualifier,
            headingDeg = smoothHeading,
            headingDisplay = ((smoothHeading % 360f + 360f) % 360f).roundToInt().coerceIn(0, 359),
            accuracyDeg = null,
            bankDeg = smoothBank.coerceIn(-90f, 90f)
        )
    }

    private fun smoothAngle(current: Float, target: Float, alpha: Float): Float =
        current + shortestDelta(current, target) * alpha

    private fun shortestDelta(current: Float, target: Float): Float {
        val raw = (target - current + 540f) % 360f - 180f
        return raw
    }

    private companion object {
        const val HEADING_ALPHA = 0.22f
        const val BANK_ALPHA = 0.30f
    }
}
