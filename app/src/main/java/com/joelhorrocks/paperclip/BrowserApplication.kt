package com.joelhorrocks.paperclip

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import android.os.Process

@HiltAndroidApp
class BrowserApplication: Application() {
    lateinit var geckoRuntime: GeckoRuntime
        private set

    override fun onCreate() {
        super.onCreate()

        val pid = Process.myPid()
        val activityManager: ActivityManager? = getSystemService()
        val isMainProcess = activityManager?.runningAppProcesses.orEmpty().any { processInfo ->
            processInfo.pid == pid && processInfo.processName == packageName
        }

        if (isMainProcess) {
            initializeGeckoRuntime()
        }
    }

    private fun initializeGeckoRuntime() {
        geckoRuntime = GeckoRuntime.create(this)
    }
}