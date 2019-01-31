package com.pajato.tmdb.lib

import java.net.URL
import java.util.zip.GZIPInputStream

actual fun fetchLines(listName: String): List<TmdbData> {
    val result = mutableListOf<TmdbData>()
    val url = getLinesUrl(listName)
    URL(url).openConnection().apply {
        readTimeout = 800
        connectTimeout = 200
    }.getInputStream().use { stream ->
        GZIPInputStream(stream).bufferedReader().forEachLine { line ->
            result.add(parse(listName, line))
        }
        return result
    }
}

/*
fun dailyOrStartupTask() {
    fun fetchLines(): Map<String, List<Animal>> {
        val map = mutableMapOf<String, List<Animal>>()
        fun getUrl(item: String) = "https://some-web-site/items/$item.gz" // to retrieve lots of lines of JSON
        fun parse(item: String, line: String) { /* code to parse and divvy up animals into object lists */ }

        animalList.forEach { item ->
            URL(getUrl(item)).openConnection().getInputStream().use { stream ->
                GZIPInputStream(stream).bufferedReader().forEachLine { line -> parse(item, line) }
            }
        }
        return map
    }

    data = fetchLines()
}
*/
