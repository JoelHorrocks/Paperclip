package com.joelhorrocks.paperclip

import android.content.res.Resources.getSystem
import androidx.compose.ui.unit.Dp
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

fun Instant.toTimeAgo(): String {
    val times = mapOf(
        TimeUnit.DAYS.toMillis(365) to "year",
        TimeUnit.DAYS.toMillis(30) to "month",
        TimeUnit.DAYS.toMillis(7) to "week",
        TimeUnit.DAYS.toMillis(1) to "day",
        TimeUnit.HOURS.toMillis(1) to "hour",
        TimeUnit.MINUTES.toMillis(1) to "minute",
    )
    val currentTime = Clock.System.now().toEpochMilliseconds()

    for ((time, ago) in times) {
        if (currentTime - this.toEpochMilliseconds() > time) {
            val diff = ((currentTime - this.toEpochMilliseconds()) / time.toFloat()).roundToInt()
            return diff.toString() + " " + ago + if(diff > 1) "s ago" else " ago"
        }
    }

    return "0 seconds ago"
}