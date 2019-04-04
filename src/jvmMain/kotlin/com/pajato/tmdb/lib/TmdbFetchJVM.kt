package com.pajato.tmdb.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.URL
import java.util.Date
import java.util.Timer
import java.util.zip.GZIPInputStream
import kotlin.concurrent.schedule

internal actual suspend fun getCacheEntry(listName: String, context: FetchContext):
        Pair<String, MutableList<TmdbData>> =
    withContext(Dispatchers.IO) {
        fun fetchData(): Pair<String, MutableList<TmdbData>> =
            URL(listName.getUrl(context)).openConnection().apply {
                readTimeout = context.readTimeoutMillis
                connectTimeout = context.connectTimeoutMillis
            }.getInputStream().use { stream ->
                val result = mutableListOf<TmdbData>()
                GZIPInputStream(stream).bufferedReader().forEachLine { line -> result.add(parse(listName, line)) }
                listName to result
            }

        try {
            fetchData()
        } catch (exc: ConnectException) {
            listName to mutableListOf<TmdbData>(TmdbError("Could not connect. See terminal output."))
        }
    }

@ExperimentalCoroutinesApi
internal actual fun scheduleNextUpdate(context: FetchContext, nextTime: Long) {
    if (context.updateAction()) {
        Timer().schedule(Date(nextTime)) { runBlocking { DatasetManager.processCacheUpdate(context, nextTime) } }
    }
}
