package com.example.afica.lovetest.di

import android.content.Context
import com.example.afica.lovetest.utils.SpManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun provideSpManager(@ApplicationContext context: Context): SpManager =
        SpManager.getInstance(context)
}
