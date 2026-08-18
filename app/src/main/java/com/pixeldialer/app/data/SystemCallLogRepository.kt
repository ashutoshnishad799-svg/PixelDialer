package com.pixeldialer.app.data

import android.content.Context
import android.provider.CallLog
import com.pixeldialer.app.data.db.CallDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SystemCallLogEntry(
    val phoneNumber: String,
    val displayName: String?,
    val direction: CallDirection,
    val timestampMillis: Long,
    val durationSeconds: Int,
    val photoUri: String?
)

/**
 * Reads the device's real call history via the system CallLog provider.
 * This is what makes old calls (made before this app was installed/default)
 * show up in Recents — without this, Recents only ever shows calls placed
 * through this app itself, which is why a fresh install looks empty even
 * after the user has called people from their previous dialer.
 *
 * Requires READ_CALL_LOG permission — callers must check before invoking.
 */
class SystemCallLogRepository(private val context: Context) {

    suspend fun loadRecentHistory(limit: Int = 300): List<SystemCallLogEntry> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SystemCallLogEntry>()

            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_PHOTO_URI
            )

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val photoIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)

                var loaded = 0
                while (loaded < limit && cursor.moveToNext()) {
                    val number = cursor.getString(numberIdx) ?: continue
                    val type = cursor.getInt(typeIdx)

                    results.add(
                        SystemCallLogEntry(
                            phoneNumber = number,
                            displayName = cursor.getString(nameIdx),
                            direction = mapCallType(type),
                            timestampMillis = cursor.getLong(dateIdx),
                            durationSeconds = cursor.getInt(durationIdx),
                            photoUri = cursor.getString(photoIdx)
                        )
                    )
                    loaded++
                }
            }
            results
        }

    private fun mapCallType(systemType: Int): CallDirection = when (systemType) {
        CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
        CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
        CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
        CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
        CallLog.Calls.BLOCKED_TYPE -> CallDirection.REJECTED
        else -> CallDirection.OUTGOING
    }
}
