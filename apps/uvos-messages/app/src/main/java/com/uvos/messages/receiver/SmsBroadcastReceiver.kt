package com.uvos.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val body = msg.displayMessageBody ?: continue
                val sender = msg.displayOriginatingAddress ?: "Unknown"

                val isSpam = isLocalSpam(body)
                if (isSpam) {
                    // Изолируем в бункер спама без всплывающего системного уведомления
                    abortBroadcast()
                }
            }
        }
    }

    private fun isLocalSpam(body: String): Boolean {
        val spamPatterns = listOf(
            "(?i).*(выигрыш|победитель|кредит без отказа|ставки|казино|онлайн-займ).*",
            "(?i).*(срочно перейдите по ссылке|вы выиграли|бонус на счет).*",
            "(?i).*http[s]?://(bit\\.ly|t\\.me/|tinyurl\\.com|cutt\\.ly).*"
        )
        return spamPatterns.any { Regex(it).matches(body) }
    }
}
