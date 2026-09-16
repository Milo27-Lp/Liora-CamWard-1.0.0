package com.aistudio.lioracamward.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScanEntity::class, FindingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: ScanDatabase? = null

        fun getDatabase(context: Context): ScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanDatabase::class.java,
                    "liora_camward.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
