package com.pajato.tmdb.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

internal actual suspend fun getCacheEntry(listName: String, url: String): Pair<String, Dataset> =
    withContext(Dispatchers.IO) {
        val context = ContextManager.context
        val datasetsDir = javaClass.getResource("/staging/")?.file
        fun fetchData(): Pair<String, Dataset> =
            URL(url).openConnection().apply {
                readTimeout = context.readTimeoutMillis
                connectTimeout = context.connectTimeoutMillis
            }.getInputStream().use { stream ->
                val outFile = File(datasetsDir, "${listName}_${context.exportDate}.json.gz")
                val length = stream.copyTo(outFile.outputStream())
                val result = Dataset(listName, outFile.absolutePath, length)
                listName to result
            }

        if (datasetsDir == null) { listName to Dataset(listName, error = "No staging directory available.") }
        else try {
            fetchData()
        } catch (exc: Exception) {
            listName to Dataset(listName, error = "Unexpected exception: ${exc.message}")
        }

    }

actual class Dataset actual constructor(
    actual val listName: String,
    actual val path: String,
    actual val length: Long,
    actual val error: String
) {
    actual fun getFirstPage(): List<TmdbData> {
        //val datasetFile = File(path)
        //val ramFile = RandomAccessFile(datasetFile, "r")
        return listOf()
    }
}

internal const val SAVED_CONTEXT_PATH = "datasets/context.json"

internal actual fun saveContext() {
    val context = ContextManager.context
    val jsonData = Json.stringify(ContextData.serializer(), ContextData(context))
    val contextFile = File(Updater.mainResourceDir, SAVED_CONTEXT_PATH)
    contextFile.writeText(jsonData)
}

internal actual fun restoreContext(): ContextData {
    val contextFile = File(Updater.mainResourceDir, SAVED_CONTEXT_PATH)
    val jsonText = contextFile.readText()
    return Json.parse(ContextData.serializer(), jsonText)
}
