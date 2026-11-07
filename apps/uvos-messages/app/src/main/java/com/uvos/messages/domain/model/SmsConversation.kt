package com.uvos.messages.domain.model

data class SmsConversation(
    val id: String,
    val address: String,
    val lastMessage: String,
    val timestamp: String,
    val isSpam: Boolean = false,
    val isOtpCode: Boolean = false
)
