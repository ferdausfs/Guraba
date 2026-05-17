package com.guraba.app.guardian.model

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed class ImportProgress {
    data object Idle : ImportProgress()
    data class Working(val percent: Int) : ImportProgress()
    data class Success(val modelName: String, val sizeBytes: Long) : ImportProgress()
    data class Error(val modelName: String, val message: String) : ImportProgress()
}

/**
 * Imports user-provided .tflite model files into the app's private filesDir.
 * Validates size and the TFL3 magic header before commit.
 */
class ModelImportManager(private val context: Context) {

    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val progress: StateFlow<ImportProgress> = _progress.asStateFlow()

    suspend fun importModel(uri: Uri, modelName: String): Result<File> = withContext(Dispatchers.IO) {
        _progress.value = ImportProgress.Working(0)
        try {
            val finalFile = File(context.filesDir, modelName)
            val tmp = File(context.filesDir, "$modelName.tmp")

            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    val msg = "Cannot open file — try a different file manager"
                    _progress.value = ImportProgress.Error(modelName, msg)
                    return@withContext Result.failure(IllegalStateException(msg))
                }
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        total += n
                        if (total > MAX_BYTES) {
                            tmp.delete()
                            val msg = "File too large (max 500 MB)"
                            _progress.value = ImportProgress.Error(modelName, msg)
                            return@withContext Result.failure(IllegalStateException(msg))
                        }
                        out.write(buf, 0, n)
                        _progress.value = ImportProgress.Working(
                            ((total * 100) / MAX_BYTES).toInt().coerceAtMost(99)
                        )
                    }
                }
            }

            if (tmp.length() < 1024) {
                tmp.delete()
                val msg = "File too small — not a valid model"
                _progress.value = ImportProgress.Error(modelName, msg)
                return@withContext Result.failure(IllegalStateException(msg))
            }

            // Validate TFL3 magic header (offset 4..8)
            tmp.inputStream().use { ins ->
                val head = ByteArray(8)
                ins.read(head)
                val magic = String(head, 4, 4, Charsets.US_ASCII)
                if (magic != "TFL3") {
                    tmp.delete()
                    val msg = "Not a valid TFLite model (magic=$magic)"
                    _progress.value = ImportProgress.Error(modelName, msg)
                    return@withContext Result.failure(IllegalStateException(msg))
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tmp.renameTo(finalFile)) {
                tmp.copyTo(finalFile, overwrite = true)
                tmp.delete()
            }
            _progress.value = ImportProgress.Success(modelName, finalFile.length())
            Result.success(finalFile)
        } catch (t: Throwable) {
            _progress.value = ImportProgress.Error(modelName, t.message ?: "error")
            Result.failure(t)
        }
    }

    fun isModelImported(modelName: String): Boolean {
        val f = File(context.filesDir, modelName)
        return f.exists() && f.length() > 0
    }

    fun modelSizeBytes(modelName: String): Long {
        val f = File(context.filesDir, modelName)
        return if (f.exists()) f.length() else 0L
    }

    fun deleteModel(modelName: String): Boolean {
        val f = File(context.filesDir, modelName)
        return if (f.exists()) f.delete() else false
    }

    companion object {
        const val MAX_BYTES: Long = 500L * 1024 * 1024
        const val MODEL_LEGACY = "guardian_model.tflite"
        const val MODEL_NSFW   = "nsfw_model.tflite"
        const val MODEL_GENDER = "gender_model.tflite"
    }
}
