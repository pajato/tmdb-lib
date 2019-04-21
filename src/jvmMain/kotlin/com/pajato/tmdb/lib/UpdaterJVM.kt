package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime.Companion.now
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.reflect.KClass

internal const val DATASETS = "datasets"
internal const val STAGING = "staging"

actual object Updater {
    internal actual val datasetCache = mutableMapOf<String, Dataset>()
    internal val mainResourceDir by lazy { getResourceDir("main") }
    internal val mainDatasetsDir = File(mainResourceDir, DATASETS)
    internal val mainStagingDir = File(mainResourceDir, STAGING)

    actual fun getDataset(listName: String): Dataset {
        fun processCacheUpdateNoTimer() {
            val context = ContextManager.context
            fun loadDatasetsFromFiles() {
                fun getPair(kclass: KClass<out TmdbData>): Pair<String, Dataset> {
                    val listNameFromClass = kclass.getListName()
                    val files = mainDatasetsDir.listFiles { file -> file.name.startsWith(listNameFromClass) }

                    return listNameFromClass to Dataset(listNameFromClass, files[0].path, files[0].length())
                }

                val tmpCache = TmdbData::class.sealedSubclasses
                    .filter { kClass ->  kClass.getListName() != "" }
                    .map { kClass -> getPair(kClass) }.toMap()
                datasetCache.putAll(tmpCache)
            }

            loadDatasetsFromFiles()
            GlobalScope.launch(Dispatchers.IO) {
                while (context.updateAction()) {
                    val nextTime = context.startTime + context.updateIntervalMillis
                    val delayInMillis = nextTime - now().unixMillisLong
                    if (delayInMillis > 0) delay(delayInMillis)
                    val tmpCache = cacheUpdateTask()
                    withContext(Dispatchers.Main) {
                        fun installDatasets() {
                            mainStagingDir.walk().iterator().forEach {
                                val destFile = File(mainDatasetsDir, it.name)
                                if (!it.isDirectory) it.renameTo(destFile)
                            }
                        }

                        Updater.datasetCache.putAll(tmpCache)
                        // next up is to promote the staging data...
                        deleteLoadedDatasets(mainDatasetsDir)
                        installDatasets()
                        saveContext()
                    }
                }
            }
        }
        fun getNotFoundErrorMessage() = "There is no dataset for the list with name '$listName'."

        if (datasetCache.isEmpty()) processCacheUpdateNoTimer()
        return datasetCache[listName] ?: Dataset(listName, error = getNotFoundErrorMessage())
    }

    internal fun deleteLoadedDatasets(dir: File) {
        dir.walk().iterator().forEach { if (!it.isDirectory) it.delete() }
    }

    internal fun getResourceDir(type: String): String =
        "${File(Updater::class.java.classLoader.getResource("").path).parent}/$type"
}
