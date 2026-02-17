package com.project.unimate

import android.app.Application

class UnimateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureAlertChannel(this)
    }
}
