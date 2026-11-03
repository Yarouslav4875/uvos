package com.uvos.gallery.domain.model

data class ExifSecurityReport(
    val threatLevel: ThreatLevel,
    val hasGpsCoordinates: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val deviceModel: String? = null,
    val timestamp: String? = null,
    val lensModel: String? = null,
    val serialNumber: String? = null,
    val software: String? = null,
    val attributesMap: Map<String, String> = emptyMap()
)
