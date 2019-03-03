package com.pajato.tmdb.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.zip.GZIPInputStream

// todo: use ktor client to make this common code.

/** Return a list using the given name. */
actual suspend fun getEntry(listName: String, baseUrl: String): Pair<String, MutableList<TmdbData>> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<TmdbData>()
        URL(getLinesUrl(listName, baseUrl)).openConnection().apply {
            readTimeout = 800
            connectTimeout = 200
        }.getInputStream().use { stream ->
            GZIPInputStream(stream).bufferedReader().forEachLine { line -> result.add(parse(listName, line)) }
            listName to result
        }
    }
