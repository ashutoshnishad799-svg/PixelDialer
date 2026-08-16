package com.pixeldialer.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val contactId: String,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String? = null
)
