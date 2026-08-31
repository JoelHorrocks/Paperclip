package com.joelhorrocks.paperclip.utils

import android.content.res.Resources.getSystem
import androidx.compose.ui.unit.Dp

fun Dp.toPx(): Float {
    return this.value * getSystem().displayMetrics.density
}

fun Float.toDp(): Dp {
    return Dp(this / getSystem().displayMetrics.density)
}

fun Int.toDp(): Dp {
    return Dp(this / getSystem().displayMetrics.density)
}