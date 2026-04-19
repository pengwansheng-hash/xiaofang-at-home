package com.xiaofangathome.senior

import android.app.Application
import com.xiaofangathome.senior.platform.SeniorAppServices

class SeniorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SeniorAppServices.initialize(this)
    }
}
