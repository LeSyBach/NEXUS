package com.example.nexus.data.di

import android.content.Context
import com.example.nexus.data.webrtc.WebRtcClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebRtcModule {
    @Provides
    @Singleton
    fun provideWebRtcClient(@ApplicationContext context: Context): WebRtcClient {
        return WebRtcClient(context)
    }
}
