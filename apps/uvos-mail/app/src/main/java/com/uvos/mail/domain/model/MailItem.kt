package com.uvos.mail.domain.model

data class MailItem(
    val id: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    val timestamp: String,
    val isPgpEncrypted: Boolean,
    val isTrackersBlocked: Boolean
)
