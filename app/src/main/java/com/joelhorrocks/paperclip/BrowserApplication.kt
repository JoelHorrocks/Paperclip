package com.joelhorrocks.paperclip

import android.app.ActivityManager
import android.app.Application
import android.os.Process
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp
import io.ktor.client.HttpClient
import org.mozilla.geckoview.GeckoRuntime

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