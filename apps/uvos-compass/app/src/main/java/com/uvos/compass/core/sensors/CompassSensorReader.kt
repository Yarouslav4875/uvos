package com.uvos.compass.core.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.uvos.compass.domain.model.OrientationSample
import com.uvos.compass.domain.model.SensorQualifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sensor data source for the course instrument.
 *
 * Strategy:
 * 1. TYPE_ROTATION_VECTOR (single fused sensor) — if present;
 * 2. fallback: TYPE_MAGNETIC_FIELD + TYPE_ACCELEROMETER fused via
 *    [SensorManager.getRotationMatrix], both channels low-pass filtered.
 *
 * Raw readings are delivered as a conflated StateFlow; aggregation and
 * damping of the displayed value live in the ViewModel layer.
 *
 * Registration lifecycle:
 * - [start] registers listeners (SENSOR_DELAY_GAME);
 * - [stop] unregisters everything;
 * both calls are idempotent. On a device without usable sensors
 * [hasAnySensor] is false, nothing is registered, and the UI shows
 * the static red lamp driven by SensorQualifier.MISSING.
 */
class CompassSensorReader(
    private val sensorManager: SensorManager?
) : SensorEventListener {

    private val rotationVector: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val hasRotationVector: Boolean = rotationVector != null
    val hasFallbackPair: Boolean = accelerometer != null && magnetometer != null
    val hasAnySensor: Boolean = hasRotationVector || hasFallbackPair

    private var running = false

    // Fallback channel state ------------------------------------------------
    private var filteredAccel: FloatArray? = null
    private var filteredMag: FloatArray? = null
    private var accelTimestampNs = 0L
    private var magTimestampNs = 0L
    private var accelAccuracy: Int = SENSOR_STATUS_UNKNOWN
    private var magAccuracy: Int = SENSOR_STATUS_UNKNOWN
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val _samples = MutableStateFlow<OrientationSample?>(value = null)
    val samples: StateFlow<OrientationSample?> = _samples.asStateFlow()

    fun start() {
        val manager = sensorManager ?: return
        if (running || !hasAnySensor) return
        running = true
        if (hasRotationVector) {
            manager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelerometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(this)
        filteredAccel = null
        filteredMag = null
        accelTimestampNs = 0L
        magTimestampNs = 0L
        accelAccuracy = SENSOR_STATUS_UNKNOWN
        magAccuracy = SENSOR_STATUS_UNKNOWN
        _samples.value = null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> accelAccuracy = accuracy
            Sensor.TYPE_MAGNETIC_FIELD -> magAccuracy = accuracy
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor?.type) {
            Sensor.TYPE_ROTATION_VECTOR -> handleRotationVector(event)
            Sensor.TYPE_ACCELEROMETER -> {
                filteredAccel = lowPass(event.values, filteredAccel, ACCEL_ALPHA)
                accelTimestampNs = event.timestamp
                tryEmitFallback()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                filteredMag = lowPass(event.values, filteredMag, MAG_ALPHA)
                magTimestampNs = event.timestamp
                tryEmitFallback()
            }
        }
    }

    private fun handleRotationVector(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        _samples.value = OrientationSample(
            headingDeg = normalizeHeading(Math.toDegrees(orientationAngles[0].toDouble()).toFloat()),
            rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat(),
            pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
            accuracy = event.accuracy
        )
    }

    private fun tryEmitFallback() {
        val accel = filteredAccel ?: return
        val mag = filteredMag ?: return
        val accelAge = (accelTimestampNs - magTimestampNs).let {
            kotlin.math.abs(it) / 1_000_000L
        }
        if (accelTimestampNs == 0L || magTimestampNs == 0L || accelAge > MAX_PAIR_AGE_MS) return

        if (!SensorManager.getRotationMatrix(rotationMatrix, null, accel, mag)) return
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        _samples.value = OrientationSample(
            headingDeg = normalizeHeading(Math.toDegrees(orientationAngles[0].toDouble()).toFloat()),
            rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat(),
            pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
            accuracy = worstAccuracy(accelAccuracy, magAccuracy)
        )
    }

    /** Maps a SENSOR_STATUS_* constant to the instrument label/lamp qualifier. */
    fun qualifierForAccuracy(accuracy: Int?): SensorQualifier = when (accuracy) {
        null -> SensorQualifier.OK
        SensorManager.SENSOR_STATUS_UNRELIABLE -> SensorQualifier.UNRELIABLE
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorQualifier.LOW_ACCURACY
        else -> SensorQualifier.OK
    }

    private fun worstAccuracy(a: Int, b: Int): Int? {
        if (a == SENSOR_STATUS_UNKNOWN || b == SENSOR_STATUS_UNKNOWN) return null
        return minOf(a, b)
    }

    /** Exponential moving average across identical channels. */
    private fun lowPass(source: FloatArray, target: FloatArray?, alpha: Float): FloatArray {
        val out = target?.clone() ?: source.clone()
        for (i in source.indices) {
            out[i] += alpha * (source[i] - out[i])
        }
        return out
    }

    private fun normalizeHeading(deg: Float): Float {
        var d = deg % 360f
        if (d < 0f) d += 360f
        return d
    }

    private companion object {
        /** No accuracy event has arrived yet for this channel. */
        const val SENSOR_STATUS_UNKNOWN = Int.MIN_VALUE

        /** Pair tolerance of accel/mag timestamps, milliseconds. */
        const val MAX_PAIR_AGE_MS = 100L

        /** Low-pass coefficients: accel moves slower, magnetometer faster. */
        const val ACCEL_ALPHA = 0.18f
        const val MAG_ALPHA = 0.25f
    }
}
