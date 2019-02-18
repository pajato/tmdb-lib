package com.pajato.tmdb.lib

import kotlinx.coroutines.*

/** Manage a global collection of TMDB data sets that are updated daily. */
@ExperimentalCoroutinesApi
object DatasetManager {

    /** Provide a cache for downloaded export data. */
    private val cache = mutableMapOf<String, List<TmdbData>>()

    /** Return a dataset for a given list name. Note that this is safe to do from the UI/Main thread. */
    suspend fun getDataset(listName: String): List<TmdbData> {
        suspend fun loadCache(): List<TmdbData> {
            val data = GlobalScope.getDataAsync()
            updateCache(data)
            return data.await()[listName] ?: listOf(TmdbError("Empty list or invalid list name: $listName!"))
        }

        return if (cache.isEmpty()) loadCache() else cache[listName] ?: listOf(TmdbError("..."))
    }

    /** Provide a never-ending coroutine that will refresh the export data set cache. */
//    suspend fun scheduleDailyFetches(timestamp: DateTime) {
//        fun getDelta(): Int /* milliseonds till noon UTC next. */ =
//            if (timestamp.hours > 12) (24 - (timestamp.hours - 12)) else (12 - timestamp.hours)
//
//        while (true) {
//            val delta = getDelta(timestamp)
//            delay(delta.toLong())
//            updateCache(GlobalScope.getDataAsync())
//        }
//    }

    /** Provide an extension operation to fetch the TMDB export data sets. */
    private fun CoroutineScope.getDataAsync(): Deferred<Map<String, List<TmdbData>>> = this.async { dailyExportIngestionTask() }

    /** Update the local cache with asynchronously fetched TMDB export data sets. */
    private suspend fun updateCache(data: Deferred<Map<String, List<TmdbData>>>) {
        data.await()
        for (entry in data.getCompleted().entries) {
            if (entry.key == ANONYMOUS) continue
            cache[entry.key] = entry.value
        }
    }
}
