package com.example.nexus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * NEXUS Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class NexusApplication : Application()
