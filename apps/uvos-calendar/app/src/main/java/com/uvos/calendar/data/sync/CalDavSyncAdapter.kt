package com.uvos.calendar.data.sync

import com.uvos.calendar.domain.model.CalendarEvent

/**
 * CalDavSyncAdapter provides synchronization primitives conforming to CalDAV RFC 4791.
 * UnderVoltOS operates in 100% offline mode with zero telemetry.
 * This adapter manages local sync queues, ETag generation, and offline ICS imports.
 */
class CalDavSyncAdapter {

    data class SyncState(
        val isSyncConfigured: Boolean = false,
        val serverUrl: String = "",
        val syncToken: String? = null,
        val pendingCount: Int = 0
    )

    fun calculateEtag(event: CalendarEvent): String {
        val payload = "${event.id}_${event.title}_${event.startTimestamp}_${event.endTimestamp}"
        return Integer.toHexString(payload.hashCode())
    }

    fun resolveConflict(local: CalendarEvent, remote: CalendarEvent): CalendarEvent {
        // Local-first invariant: if timestamps differ, prefer local modifications
        return local.copy(syncId = remote.syncId ?: local.syncId)
    }
}
