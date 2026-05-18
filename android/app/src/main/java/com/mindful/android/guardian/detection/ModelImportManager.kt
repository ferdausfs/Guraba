package com.mindful.android.guardian.detection

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class ImportProgress {
    data object Idle : ImportProgress()
    data class Working(val percent: Int) : ImportProgress()
    data class Success(val modelName: String, val sizeBytes: Long) : ImportProgress()
    data class Error(val modelName: String, val message: String) : ImportProgress()
}

@Singleton
class ModelImportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val progress: StateFlow<ImportProgress> = _progress.asStateFlow()

    suspend fun importModel(uri: Uri, modelName: String): Result<File> = withContext(Dispatchers.IO) {
        _progress.value = ImportProgress.Working(0)
        try {
            val finalFile = File(context.filesDir, modelName)
            val tmp = File(context.filesDir, "$modelName.tmp")
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    val msg = "Cannot open file"
                    _progress.value = ImportProgress.Error(modelName, msg)
                    return@withContext Result.failure(IllegalStateException(msg))
                }
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024); var total = 0L
                    while (true) {
                        val n = input.read(buf); if (n <= 0) break
                        total += n
                        if (total > MAX_BYTES) {
                            tmp.delete()
                            _progress.value = ImportProgress.Error(modelName, "File too large (max 500 MB)")
                            return@withContext Result.failure(IllegalStateException("Too large"))
                        }
                        out.write(buf, 0, n)
                        _progress.value = ImportProgress.Working(((total * 100) / MAX_BYTES).toInt().coerceAtMost(99))
                    }
                }
            }
            if (tmp.length() < 1024) { tmp.delete(); _progress.value = ImportProgress.Error(modelName, "File too small"); return@withContext Result.failure(IllegalStateException("Too small")) }
            if (finalFile.exists()) finalFile.delete()
            if (!tmp.renameTo(finalFile)) { tmp.copyTo(finalFile, overwrite = true); tmp.delete() }
            _progress.value = ImportProgress.Success(modelName, finalFile.length())
            Result.success(finalFile)
        } catch (t: Throwable) {
            Timber.e(t, "Import failed for $modelName")
            _progress.value = ImportProgress.Error(modelName, t.message ?: "error")
            Result.failure(t)
        }
    }

    fun isModelImported(modelName: String) = File(context.filesDir, modelName).let { it.exists() && it.length() > 0 }
    fun modelSizeBytes(modelName: String) = File(context.filesDir, modelName).let { if (it.exists()) it.length() else 0L }
    fun deleteModel(modelName: String) = File(context.filesDir, modelName).let { if (it.exists()) it.delete() else false }

    companion object { const val MAX_BYTES: Long = 500L * 1024 * 1024 }
}
