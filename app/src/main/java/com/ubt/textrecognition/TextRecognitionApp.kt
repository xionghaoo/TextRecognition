package com.ubt.textrecognition

import android.app.Application
import timber.log.Timber

class TextRecognitionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}