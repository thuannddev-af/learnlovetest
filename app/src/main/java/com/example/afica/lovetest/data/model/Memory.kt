package com.example.afica.lovetest.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey val id: String,
    val title: String,
    val date: Long,
    val photoPath: String
)
