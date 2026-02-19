package com.project.unimate

import android.app.Application

class UnimateApp : Application() {
    companion object {
        lateinit var instance: UnimateApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationChannels.ensureAlertChannel(this)
    }
}
