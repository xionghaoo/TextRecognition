package com.ubt.textrecognition

import android.app.Application
import timber.log.Timber

class TextRecognitionApp : Application() {

    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("native_lib")
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}