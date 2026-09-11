package com.joelhorrocks.paperclip.ml

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtUtil
import ai.onnxruntime.platform.Fp16Conversions
import android.util.Log
import androidx.collection.longListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.flatten
import kotlin.math.exp


// TODO: interface, objects for each model rather than just path (modular from just ONNX)
// TODO: handle model deletion during translation
@Singleton
class Translator @Inject constructor(
    private val ortEnvironment: OrtEnvironment,
    // TODO: make sure model directory is consistent
    private val modelDirectory: Path
) {
    private val sessions = mutableMapOf<String, Pair<OrtSession, OrtSession>>()

    private fun encodeWord(word: String, tokenizerMap: Map<String, Float>): List<String> {
        val bestSegmentations = mutableListOf<Pair<Int?, Int?>>()
        bestSegmentations.add(Pair(0, 1))

        for(i in 0.until(word.length)) {
            bestSegmentations.add(Pair(null, null))
        }

        for(startIdx in 0.until(word.length)) {
            val bestScoreAtStart = bestSegmentations[startIdx].second

            for(endIdx in (startIdx + 1)..word.length) {
                val token = word.substring(startIdx, endIdx)

                if(tokenizerMap.keys.contains(token) && bestScoreAtStart != null) {
                    val score = tokenizerMap[token]!! + bestScoreAtStart

                    if(bestSegmentations[endIdx].second == null || bestSegmentations[endIdx].second!! < score) {
                        bestSegmentations[endIdx] = Pair(startIdx, endIdx)
                    }
                }
            }
        }

        val segmentation = bestSegmentations.last()
        if(segmentation.second == null) {
            return listOf("<unk>")
        }

        var start = segmentation.first
        var end = word.length
        val tokens = mutableListOf<String>()

        while(start != 0) {
            val token = word.substring(start!!, end)
            tokens.add(0, token)

            val nextStart = bestSegmentations[start].first
            end = start
            start = nextStart
        }

        tokens.add(0, word.substring(start, end))
        return tokens
    }

    // TODO: should we key by path?
    // TODO: dispatcher as parameter
    // TODO: KV cache
    // TODO: beam search
    suspend fun translate(path: String, text: String): String = withContext(Dispatchers.Default) {
        // TODO: proper path handling
        val directory = File(modelDirectory.toFile(), path)
        val json = Json { ignoreUnknownKeys = true }

        // TODO: split into methods
        val unigramModel = File(directory, "unigram.json")

        // TODO: handle JSON properly
        // TODO: store in a better way (in JSON file)
        val tokenizerMap = mutableMapOf<String, Float>()
        val unigramModelText = withContext(Dispatchers.IO) { unigramModel.readText() }
        val tokenizerList = json.parseToJsonElement(unigramModelText).jsonObject["vocab"]!!.jsonArray

        for (tokenizerItem in tokenizerList) {
            tokenizerMap[tokenizerItem.jsonObject["token"]!!.jsonPrimitive.content] =
                tokenizerItem.jsonObject["score"]!!.jsonPrimitive.float
        }

        val vocab = File(directory, "vocab.json")

        val vocabText = withContext(Dispatchers.IO) { vocab.readText() }
        val vocabMap = json.parseToJsonElement(vocabText).jsonObject.toMap().mapValues { it.value.jsonPrimitive.content.toLong() }
        val revVocabMap = mutableMapOf<Int, String>()

        for(entry in vocabMap) {
            revVocabMap[entry.value.toInt()] = entry.key
        }
        // TODO: check if we should replace \n or it causes issues (we got <unk> when input could have newlines)
        val splitText = text.replace("\n", "").split(" ").map { "▁$it" }
        val tokens = splitText.flatMap { encodeWord(it, tokenizerMap) }
        val inputTokens = tokens.map { vocabMap.getOrDefault(it, 1) }.plus(0)

        val encoderModel = File(directory, "encoder_model.onnx")
        val decoderModel = File(directory, "decoder_model_merged.onnx")

        var encoderSession: OrtSession
        var decoderSession: OrtSession

        if(sessions[path] == null) {
            encoderSession = ortEnvironment.createSession(encoderModel.path)
            decoderSession = ortEnvironment.createSession(decoderModel.path)

            // TODO: evict old sessions on low memory?
            sessions[path] = Pair(encoderSession, decoderSession)
        } else {
            encoderSession = sessions[path]!!.first
            decoderSession = sessions[path]!!.second
        }

        val encoderInputIds = OnnxTensor.createTensor(ortEnvironment, LongBuffer.wrap(inputTokens.toLongArray()), longArrayOf(1, inputTokens.size.toLong()))
        val encoderAttentionMask = OnnxTensor.createTensor(ortEnvironment, LongBuffer.wrap(LongArray(inputTokens.size) { 1 }), longArrayOf(1, inputTokens.size.toLong()))

        val encoderInputs: Map<String, OnnxTensor> = mapOf(
            "input_ids" to encoderInputIds,
            "attention_mask" to encoderAttentionMask
        )

        val encoderSessionOutput = encoderSession.run(encoderInputs)
        val encoderOutput: Array<Array<FloatArray>> = encoderSessionOutput.get("last_hidden_state").get().value as Array<Array<FloatArray>>

        encoderInputIds.close()

        val decoderOutputList = mutableListOf(0L)

        val useCacheBranch = OnnxTensor.createTensor(
            ortEnvironment,
            ByteBuffer.wrap(byteArrayOf(0)),
            longArrayOf(1),
            OnnxJavaType.BOOL
        )
        val decoderInputIds = OnnxTensor.createTensor(
            ortEnvironment,
            LongBuffer.wrap(decoderOutputList.toLongArray()),
            longArrayOf(1, 1)
        )
        val encoderHiddenStates = OnnxTensor.createTensor(
            ortEnvironment,
            Fp16Conversions.convertFloatBufferToFp16Buffer(
                FloatBuffer.wrap(
                    encoderOutput.flatten().map {
                        it.toTypedArray()
                    }.toTypedArray().flatten().toFloatArray())
            ),
            longArrayOf(1, inputTokens.size.toLong(), 256),
            OnnxJavaType.FLOAT16
        )

        val decoderInputs: MutableMap<String, OnnxTensor> = mutableMapOf(
            "use_cache_branch" to useCacheBranch,
            "input_ids" to decoderInputIds,
            "encoder_attention_mask" to encoderAttentionMask,
            "encoder_hidden_states" to encoderHiddenStates
        )

        while (
            (decoderOutputList.last() != 0L ||
                    decoderOutputList.size == 1) &&
            decoderOutputList.size < 256
        ) {
            val decoderSessionOutput = decoderSession.run(decoderInputs)
            val decoderOutput: Array<Array<FloatArray>> =
                decoderSessionOutput.get("logits")
                    .get().value as Array<Array<FloatArray>>

            decoderSessionOutput.close()
            decoderInputs["input_ids"]?.close()

            // TODO: check decoderOutput
            val logits = decoderOutput[0].last()
            var maxLogitIdx = 0

            for (i in 0.until(logits.size)) {
                if(logits[i] > logits[maxLogitIdx]) {
                    maxLogitIdx = i
                }
            }

            decoderOutputList.add(maxLogitIdx.toLong())

            decoderInputs["input_ids"] = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(decoderOutputList.toLongArray()),
                longArrayOf(1, decoderOutputList.size.toLong())
            )
        }

        // TODO: change so resources are properly closed if coroutine is cancelled
        useCacheBranch.close()
        encoderAttentionMask.close()
        decoderInputs["input_ids"]?.close()
        encoderHiddenStates.close()

        decoderOutputList.subList(1, decoderOutputList.size - 1).map { revVocabMap[it.toInt()] }.joinToString("").replace("▁", " ")
    }

    fun handleDelete(path: String) {
        // TODO: cancel any translate jobs
    }
}