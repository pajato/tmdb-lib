package com.pajato.tmdb.lib

import kotlinx.coroutines.*
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.reflect.KClass

// Provide a JVM implementation for the daily export task.
actual fun dailyExportTask(): Map<String, List<TmdbData>> {

    // TODO: if you make this function suspend you can just remove runBlocking
    return runBlocking {
        TmdbData::class.sealedSubclasses
            .map { kClass ->
                async { fetchLines(kClass) }
            }
            .awaitAll()
            // TODO: It may be any nullability handling policy, now just ignore nulls
            .filterNotNull()
            .toMap()
            .also {
                println("Done with daily export task!")
            }
    }
}

// Move blocking call to IO dispatcher to make suspend function non-blocking
private suspend fun fetchLines(subclass: KClass<out TmdbData>): Pair<String, MutableList<TmdbData>>? = withContext(Dispatchers.IO) {
    //TODO: we probably should just throw exception, because class without name is probably an error
    val listName = getListName(subclass) ?: return@withContext null

    val result = mutableListOf<TmdbData>()
    val url = getLinesUrl(listName)
    URL(url).openConnection().apply {
        readTimeout = 800
        connectTimeout = 200
    }.getInputStream().use { stream ->
        GZIPInputStream(stream).bufferedReader().forEachLine { line ->
            result.add(parse(listName, line))
        }
        listName to result
    }
}

private fun getListName(subclass: KClass<out TmdbData>): String? {
    fun getListNameFromDefault(item: TmdbData): String = item.getListName()
    val name = subclass.simpleName ?: return null
    val listName = getListNameFromDefault(createDefaultFromType(name))

    return if (listName.isNotBlank()) listName else null
}