package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SubjectResourceEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(PharmacyConverters::class)
abstract class PharmacyDatabase : RoomDatabase() {
    abstract fun subjectResourceDao(): SubjectResourceDao

    companion object {
        @Volatile
        private var INSTANCE: PharmacyDatabase? = null

        fun getInstance(context: Context): PharmacyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PharmacyDatabase::class.java,
                    "yes_perfect_pharmacy.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
