package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime.Companion.now
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Manage a global collection of TMDB data sets that are updated daily. */
@ExperimentalCoroutinesApi
object DatasetManager {
    /** Provide a cache for downloaded export data. */
    private val cache = mutableMapOf<String, List<TmdbData>>()

    /** Provide a test only operation to clear the cache. */
    internal fun resetCache() = cache.clear()

    /** Return a dataset for a given list name. Note that this is safe to do from the UI/Main thread. */
    suspend fun getDataset(listName: String, config: FetchConfig = FetchConfigImpl()): List<TmdbData> =
        if (cache.isEmpty()) scheduleDailyFetches(listName, config) else cache[listName] ?: listOf(TmdbError("..."))

    /** Provide a never-ending coroutine that will refresh the export data set cache. */
    private suspend fun scheduleDailyFetches(listName: String, config: FetchConfig): List<TmdbData> {
        // Update the cache starting with the configured date and executing subsequent updates at the configured
        // interval.
        val result = mutableListOf<TmdbData>()

        while (processDailyUpdate(listName, config, now().unixMillisLong, result)) {}
        return result
    }

    /** Recursively update the dataset cache until the configured number of cycles is reached (possibly infinite). */
    private tailrec suspend fun processDailyUpdate(
        listName: String,
        config: FetchConfig,
        startTime: Long,
        result: MutableList<TmdbData>
    ): Boolean {
        suspend fun loadCache(listName: String, config: FetchConfig): List<TmdbData> {
            suspend fun updateCache(data: Deferred<Map<String, List<TmdbData>>>) {
                data.await()
                for (entry in data.getCompleted().entries) {
                    if (entry.key == ANONYMOUS) continue
                    cache[entry.key] = entry.value
                }
            }

            val data = coroutineScope { async { dailyCacheRefreshTask(config) } }
            updateCache(data)
            return data.await()[listName] ?: listOf(TmdbError("Empty list or invalid list name: $listName!"))
        }

        /** Return the objects for the given list as soon as it has been fetched and load/update the dataset cache. */
        return if (!config.updateAction()) false else {
            val nextTime = startTime + config.updateInterval
            result.clear()
            result.addAll(loadCache(listName, config))
            processDailyUpdate(listName, config, nextTime, result)
        }
    }
}
