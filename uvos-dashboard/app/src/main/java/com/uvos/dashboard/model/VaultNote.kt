package com.uvos.dashboard.model

/**
 * VaultNote - защищенная запись в аналоговом сейфе UVOS.
 */
data class VaultNote(
    val id: Long,
    val title: String,
    val content: String,
    val timestamp: Long
)
