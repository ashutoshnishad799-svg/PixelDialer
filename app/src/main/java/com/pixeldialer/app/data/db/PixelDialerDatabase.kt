package com.pixeldialer.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CallLogEntity::class, BlockedNumberEntity::class, FavoriteEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PixelDialerDatabase : RoomDatabase() {

    abstract fun callLogDao(): CallLogDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: PixelDialerDatabase? = null

        fun getInstance(context: Context): PixelDialerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PixelDialerDatabase::class.java,
                    "pixel_dialer.db"
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
