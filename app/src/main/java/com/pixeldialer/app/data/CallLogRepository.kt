package com.pixeldialer.app.data

import com.pixeldialer.app.data.db.CallDirection
import com.pixeldialer.app.data.db.CallLogDao
import com.pixeldialer.app.data.db.CallLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CallLogRepository(private val dao: CallLogDao) {

    /** Groups consecutive calls to/from the same number, mirroring stock dialer "(N)" grouping. */
    fun observeGroupedRecents(): Flow<List<RecentCall>> =
        dao.observeAll().map { entries -> groupConsecutive(entries) }

    fun observeMissed(): Flow<List<RecentCall>> =
        dao.observeMissed().map { entries -> groupConsecutive(entries) }

    private fun groupConsecutive(entries: List<CallLogEntity>): List<RecentCall> {
        if (entries.isEmpty()) return emptyList()
        val result = mutableListOf<RecentCall>()
        var i = 0
        while (i < entries.size) {
            val current = entries[i]
            var count = 1
            var j = i + 1
            while (j < entries.size && entries[j].phoneNumber == current.phoneNumber) {
                count++
                j++
            }
            result.add(
                RecentCall(
                    id = current.id,
                    displayName = current.displayName ?: current.phoneNumber,
                    phoneNumber = current.phoneNumber,
                    direction = current.direction,
                    timestampMillis = current.timestampMillis,
                    durationSeconds = current.durationSeconds,
                    photoUri = current.photoUri,
                    callCount = count,
                    isSpam = current.isSpam
                )
            )
            i = j
        }
        return result
    }

    suspend fun logCall(
        number: String,
        name: String?,
        direction: CallDirection,
        durationSeconds: Int = 0,
        photoUri: String? = null,
        isSpam: Boolean = false
    ) {
        dao.insert(
            CallLogEntity(
                phoneNumber = number,
                displayName = name,
                direction = direction,
                timestampMillis = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                photoUri = photoUri,
                isSpam = isSpam
            )
        )
    }

    suspend fun clearHistory() = dao.clearAll()
}
