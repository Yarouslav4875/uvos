package com.uvos.calendar.data.sync

import com.uvos.calendar.domain.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object IcsParser {

    private val icsDateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val icsAllDayFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Serializes a list of CalendarEvents into standard RFC 5545 .ics format
     */
    fun exportToIcs(events: List<CalendarEvent>): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//UnderVoltOS//UVOS Calendar 1.0//RU\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")

        val nowStr = icsDateFormat.format(Date())

        for (event in events) {
            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:").append(event.syncId ?: "${UUID.randomUUID()}@uvos.local").append("\r\n")
            sb.append("DTSTAMP:").append(nowStr).append("\r\n")

            if (event.isAllDay) {
                sb.append("DTSTART;VALUE=DATE:").append(icsAllDayFormat.format(Date(event.startTimestamp))).append("\r\n")
                sb.append("DTEND;VALUE=DATE:").append(icsAllDayFormat.format(Date(event.endTimestamp))).append("\r\n")
            } else {
                sb.append("DTSTART:").append(icsDateFormat.format(Date(event.startTimestamp))).append("\r\n")
                sb.append("DTEND:").append(icsDateFormat.format(Date(event.endTimestamp))).append("\r\n")
            }

            sb.append("SUMMARY:").append(escapeIcsText(event.title)).append("\r\n")

            if (!event.description.isNullOrBlank()) {
                sb.append("DESCRIPTION:").append(escapeIcsText(event.description)).append("\r\n")
            }
            if (!event.location.isNullOrBlank()) {
                sb.append("LOCATION:").append(escapeIcsText(event.location)).append("\r\n")
            }
            if (!event.recurrenceRule.isNullOrBlank()) {
                val rruleVal = if (event.recurrenceRule.startsWith("RRULE:")) {
                    event.recurrenceRule
                } else {
                    "RRULE:${event.recurrenceRule}"
                }
                sb.append(rruleVal).append("\r\n")
            }

            for (reminder in event.reminders) {
                sb.append("BEGIN:VALARM\r\n")
                sb.append("ACTION:DISPLAY\r\n")
                sb.append("DESCRIPTION:Напоминание\r\n")
                sb.append("TRIGGER:-PT").append(reminder.minutesBefore).append("M\r\n")
                sb.append("END:VALARM\r\n")
            }

            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    /**
     * Parses standard RFC 5545 .ics text into a list of CalendarEvent domain objects
     */
    fun parseIcs(icsContent: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val lines = unfoldLines(icsContent)

        var insideEvent = false
        var uid: String? = null
        var summary = "Событие"
        var description: String? = null
        var location: String? = null
        var startTs = 0L
        var endTs = 0L
        var isAllDay = false
        var rrule: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true)) {
                insideEvent = true
                uid = null
                summary = "Событие"
                description = null
                location = null
                startTs = System.currentTimeMillis()
                endTs = startTs + 3600_000L
                isAllDay = false
                rrule = null
                continue
            }

            if (trimmed.equals("END:VEVENT", ignoreCase = true)) {
                if (insideEvent) {
                    events.add(
                        CalendarEvent(
                            title = summary,
                            description = description,
                            location = location,
                            startTimestamp = startTs,
                            endTimestamp = if (endTs > startTs) endTs else startTs + 3600_000L,
                            isAllDay = isAllDay,
                            recurrenceRule = rrule,
                            colorHex = "#E02020",
                            syncId = uid
                        )
                    )
                }
                insideEvent = false
                continue
            }

            if (!insideEvent) continue

            val colonIdx = trimmed.indexOf(':')
            if (colonIdx == -1) continue

            val propKey = trimmed.substring(0, colonIdx)
            val propVal = unescapeIcsText(trimmed.substring(colonIdx + 1))

            when {
                propKey.startsWith("UID", ignoreCase = true) -> uid = propVal
                propKey.startsWith("SUMMARY", ignoreCase = true) -> summary = propVal
                propKey.startsWith("DESCRIPTION", ignoreCase = true) -> description = propVal
                propKey.startsWith("LOCATION", ignoreCase = true) -> location = propVal
                propKey.startsWith("RRULE", ignoreCase = true) -> rrule = propVal
                propKey.startsWith("DTSTART", ignoreCase = true) -> {
                    if (propKey.contains("VALUE=DATE", ignoreCase = true)) {
                        isAllDay = true
                        parseDateOnly(propVal)?.let { startTs = it }
                    } else {
                        parseDateTime(propVal)?.let { startTs = it }
                    }
                }
                propKey.startsWith("DTEND", ignoreCase = true) -> {
                    if (propKey.contains("VALUE=DATE", ignoreCase = true)) {
                        parseDateOnly(propVal)?.let { endTs = it }
                    } else {
                        parseDateTime(propVal)?.let { endTs = it }
                    }
                }
            }
        }

        return events
    }

    private fun unfoldLines(content: String): List<String> {
        val rawLines = content.replace("\r\n", "\n").split('\n')
        val result = mutableListOf<String>()
        val currentLine = StringBuilder()

        for (line in rawLines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                currentLine.append(line.substring(1))
            } else {
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    currentLine.setLength(0)
                }
                currentLine.append(line)
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return result
    }

    private fun parseDateTime(value: String): Long? {
        return try {
            icsDateFormat.parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDateOnly(value: String): Long? {
        return try {
            icsAllDayFormat.parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun escapeIcsText(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }

    private fun unescapeIcsText(text: String): String {
        return text.replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}
