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

    /**
     * Pulls the device's real call history (from the system CallLog provider,
     * via [SystemCallLogRepository]) into our local Room cache. This is what
     * makes calls made *before* this app was installed — or before it was
     * default dialer — appear in Recents. Safe to call repeatedly: the
     * (phoneNumber, timestampMillis) unique index means already-synced
     * entries are silently ignored rather than duplicated.
     */
    suspend fun syncFromSystem(systemRepo: SystemCallLogRepository) {
        val systemEntries = systemRepo.loadRecentHistory()
        if (systemEntries.isEmpty()) return

        val asEntities = systemEntries.map { entry ->
            CallLogEntity(
                phoneNumber = entry.phoneNumber,
                displayName = entry.displayName,
                direction = entry.direction,
                timestampMillis = entry.timestampMillis,
                durationSeconds = entry.durationSeconds,
                photoUri = entry.photoUri
            )
        }
        dao.insertAll(asEntities)
    }

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

    /** One-shot snapshot of the most recent entries, used when pushing a cloud backup. */
    suspend fun rawEntriesForBackup(limit: Int = 500): List<CallLogEntity> =
        dao.getRecentSnapshot(limit)

    /**
     * Replaces local call history with a restored cloud snapshot. Uses the
     * same insertAll(IGNORE) path as the system-history sync, so entries
     * that already exist locally (matching phoneNumber+timestampMillis)
     * are left alone rather than duplicated — a restore is additive with
     * respect to anything the device already logged since the backup.
     */
    suspend fun replaceAllFromBackup(entries: List<CallLogEntity>) {
        if (entries.isEmpty()) return
        // Room's @Insert autoGenerate primary key means passing entries with
        // their remote `id` values would collide with local ids; strip them
        // so each restored row gets a fresh local id and relies on the
        // (phoneNumber, timestampMillis) unique index for de-duplication.
        val withoutIds = entries.map { it.copy(id = 0) }
        dao.insertAll(withoutIds)
    }
}
