package com.joelhorrocks.paperclip.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton


// TODO: interface, objects for each model rather than just path (modular from just ONNX)
@Singleton
class Translator @Inject constructor(
    private val ortEnvironment: OrtEnvironment,
    // TODO: make sure model directory is consistent
    private val modelDirectory: Path
) {
    private val sessions = mutableMapOf<String, OrtSession>()

    // TODO: should we key by path?
    suspend fun translate(path: String, text: String): String {
        // TODO: implement
        return ""
    }

    fun handleDelete(path: String) {
        // TODO: cancel any translate jobs
    }
}