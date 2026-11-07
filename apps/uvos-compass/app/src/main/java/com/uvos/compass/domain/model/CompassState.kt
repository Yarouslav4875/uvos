package com.uvos.compass.domain.model

/**
 * Raw orientation result produced by the sensor layer, degrees:
 * - [headingDeg]: magnetic azimuth 0..360 (0 = North).
 * - [rollDeg]: bank angle, -180..180.
 * - [pitchDeg]: pitch angle, -180..180.
 * - [accuracy]: sensor accuracy status constant
 *   (SensorManager.SENSOR_STATUS_*), null when unknown.
 */
data class OrientationSample(
    val headingDeg: Float,
    val rollDeg: Float,
    val pitchDeg: Float,
    val accuracy: Int?
)

/**
 * Aggregated, filtered UI state of the instrument.
 *
 * Availability/quality of the sensors is reported in [sensorQualifier]:
 * - [SensorQualifier.OK] — sensor present, accuracy acceptable;
 * - [SensorQualifier.LOW_ACCURACY] — sensor present, accuracy low;
 * - [SensorQualifier.UNRELIABLE] — sensor reported UNRELIABLE status;
 * - [SensorQualifier.MISSING] — no usable sensor on the device.
 */
enum class SensorQualifier { OK, LOW_ACCURACY, UNRELIABLE, MISSING }

data class CompassUiState(
    val qualifier: SensorQualifier = SensorQualifier.MISSING,
    val headingDeg: Float = 0f,
    val headingDisplay: Int = 0,
    val accuracyDeg: Float? = null,
    val bankDeg: Float = 0f
)
