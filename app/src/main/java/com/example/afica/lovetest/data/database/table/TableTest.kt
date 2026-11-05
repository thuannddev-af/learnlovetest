package com.example.afica.lovetest.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "table_test")
class TableTest(
    @PrimaryKey
    val id: Int,
    val title: String
)