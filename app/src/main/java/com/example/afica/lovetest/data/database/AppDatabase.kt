package com.example.afica.lovetest.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.afica.lovetest.data.database.table.TableTest
import com.example.afica.lovetest.data.database.dao.MemoryDao
import com.example.afica.lovetest.data.model.Memory
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TableTest::class, Memory::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase()
{
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS memories (id TEXT NOT NULL, title TEXT NOT NULL, date INTEGER NOT NULL, photoPath TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }
    }
    abstract fun memoryDao(): MemoryDao
}
