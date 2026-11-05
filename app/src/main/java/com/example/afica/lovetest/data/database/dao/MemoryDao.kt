package com.example.afica.lovetest.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.afica.lovetest.data.model.Memory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY date DESC, id DESC")
    fun observeMemories(): Flow<List<Memory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(memory: Memory)
}
