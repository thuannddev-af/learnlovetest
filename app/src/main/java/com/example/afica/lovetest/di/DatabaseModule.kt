package com.example.afica.lovetest.di

import com.example.afica.lovetest.utils.Constant

import android.content.Context
import androidx.room.Room
import com.example.afica.lovetest.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, Constant.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
}
