package com.pajato.tmdb.lib

import kotlinx.coroutines.*
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.reflect.KClass

/** Provide a JVM implementation for the daily export task. */
actual suspend fun dailyExportTask(): Map<String, List<TmdbData>> = coroutineScope {
    // Asynchronously import the export data set for each TmdbData subclass.
    TmdbData::class.sealedSubclasses
        .map { kClass -> async { fetchLines(kClass) } }.awaitAll().toMap()
        .also { println("Done with daily export task!") }
}

/** Fetch and parse the export data set records for the given TmdbData subclass. */
private suspend fun fetchLines(subclass: KClass<out TmdbData>): Pair<String, MutableList<TmdbData>> =
    withContext(Dispatchers.IO) {
        val listName = getListName(subclass)
        if (listName == ANONYMOUS) {
            ANONYMOUS to mutableListOf<TmdbData>(TmdbError("Unexpected anonymous TMDB subclass!"))
        } else {
            val result = mutableListOf<TmdbData>()
            URL(getLinesUrl(listName)).openConnection().apply {
                readTimeout = 800
                connectTimeout = 200
            }.getInputStream().use { stream ->
                GZIPInputStream(stream).bufferedReader().forEachLine { line -> result.add(parse(listName, line)) }
                listName to result
            }
        }
    }

/** Return a TMDB export data set list name for a given subclass. */
private fun getListName(subclass: KClass<out TmdbData>): String {
    val name = subclass.simpleName ?: return ANONYMOUS
    return createDefaultFromType(name).getListName()
}
