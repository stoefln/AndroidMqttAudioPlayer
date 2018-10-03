package com.stephanpetzl.mqttaudioplayer

import android.app.Application
import android.util.Log
import timber.log.Timber

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }

}