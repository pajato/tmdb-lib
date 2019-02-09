package com.pajato.tmdb.lib

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.reflect.KClass

// Provide a JVM implementation for the daily export data sets.
@ExperimentalCoroutinesApi
actual fun updateExportData(data: MutableMap<String, List<TmdbData>>) {
    val channel = Channel<List<TmdbData>>()
    fun fetchListForClass(scope: CoroutineScope, kClass: KClass<out TmdbData>) {
        fun getListName(subclass: KClass<out TmdbData>): String? {
            fun getListNameFromDefault(item: TmdbData): String = item.getListName()
            val name = subclass.simpleName ?: return null
            val listName = getListNameFromDefault(createDefaultFromType(name))

            return if (listName.isNotBlank()) listName else null
        }
        fun fetchLines(listName: String): List<TmdbData> {
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

        val name = getListName(kClass) ?: return
        scope.launch(Dispatchers.IO) {
            val list = fetchLines(name)
            if (list.isNotEmpty()) { channel.send(list) }
        }

    }

    // Kick off a number of coroutines, one each to handle an export data set class.
    runBlocking {
        TmdbData::class.sealedSubclasses.forEach { kClass -> fetchListForClass(this, kClass) }
        for (i in TmdbData::class.sealedSubclasses.size - 2 downTo 0 ) {
            val list = channel.receive()
            val name = list[0].getListName()
            println("Processing list {$name} of size: ${list.size}; i == $i.")
        }
    }
}
