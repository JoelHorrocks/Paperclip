package com.joelhorrocks.paperclip.utils

import java.io.File
import kotlin.math.log10
import kotlin.math.pow

fun Long.toFileSize(): String {
    if(this <= 0) {
        return "0B"
    }

    val sizes = listOf("B", "KB", "MB", "GB", "TB")
    val sizeUnit = (log10(this.toDouble()) / 3).toInt()

    val size = this / (10F.pow(sizeUnit * 3))

    if(sizeUnit >= sizes.size) {
        return ">1${sizes.last()}"
    } else {
        return String.format("%.1f%s", size, sizes[sizeUnit])
    }
}

fun File.getSize(): Long {
    var size: Long = 0
    if (this.isDirectory) {
        for (file in this.listFiles()!!) {
            size += file.getSize()
        }
    } else {
        size = this.length()
    }
    return size
}
