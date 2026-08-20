package com.pixeldialer.app.data

data class Contact(
    val id: String,
    val contactId: String = id,
    val displayName: String,
    val phoneNumber: String,
    val numberLabel: String = "Mobile",
    val photoUri: String? = null,
    val isFavorite: Boolean = false
)

data class RecentCall(
    val id: Long,
    val displayName: String,
    val phoneNumber: String,
    val direction: com.pixeldialer.app.data.db.CallDirection,
    val timestampMillis: Long,
    val durationSeconds: Int,
    val photoUri: String? = null,
    val callCount: Int = 1,
    val isSpam: Boolean = false
)
