package com.pajato.tmdb.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.URL
import java.util.zip.GZIPInputStream

/** Return a list using the given name handling any exceptions. */
internal actual suspend fun getEntry(listName: String, config: FetchConfig): Pair<String, MutableList<TmdbData>> =
    withContext(Dispatchers.IO) {
        try {
            listName.fetchData(config)
        } catch (exc: ConnectException) {
            listName to mutableListOf<TmdbData>(TmdbError("Could not connect. See terminal output."))
        }
    }

/** Return a list for the receiver possibly throwing a connection exception. */
private fun String.fetchData(config: FetchConfig): Pair<String, MutableList<TmdbData>> =
    URL(config.getUrl(this)).openConnection().apply {
        readTimeout = config.readTimeout
        connectTimeout = config.connectTimeout
    }.getInputStream().use { stream ->
        val result = mutableListOf<TmdbData>()
        GZIPInputStream(stream).bufferedReader().forEachLine { line -> result.add(parse(this, line)) }
         this to result
    }
