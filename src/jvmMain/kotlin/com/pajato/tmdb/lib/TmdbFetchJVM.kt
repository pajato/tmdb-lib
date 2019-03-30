package com.pajato.tmdb.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.URL
import java.util.zip.GZIPInputStream

/** Return a list using the given name handling any exceptions. */
internal actual suspend fun getEntry(listName: String, context: FetchContext): Pair<String, MutableList<TmdbData>> =
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
