package com.project.unimate

// 역할: Application 진입점. 싱글톤 instance, 알림 채널 초기화

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
