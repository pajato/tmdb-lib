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
    suspend fun getDataset(listName: String, context: FetchContext = ContextImpl()): List<TmdbData> {
        if (cache.isEmpty()) scheduleDailyFetches(context)
        return cache[listName] ?: listOf(TmdbError("..."))
    }

    /** Provide a possibly never-ending coroutine that will refresh the export data set cache. */
    private suspend fun scheduleDailyFetches(context: FetchContext) {
        // Update the cache starting with the configured date and executing subsequent updates at the configured
        // interval.
        //val foo = processDailyUpdate(context, now().unixMillisLong)
        processDailyUpdate(context, now().unixMillisLong)
    }

    /** Recursively update the dataset cache until the configured number of cycles is reached (possibly infinite). */
    private tailrec suspend fun processDailyUpdate(context: FetchContext, startTime: Long) {
        suspend fun loadCache() {
            suspend fun updateCache(data: Deferred<Map<String, List<TmdbData>>>) {
                data.await()
                for (entry in data.getCompleted().entries) {
                    if (entry.key == ANONYMOUS) continue
                    cache[entry.key] = entry.value
                }
            }

            val data = coroutineScope { async { dailyCacheRefreshTask(context) } }
            updateCache(data)
        }

        // Handle the scheduling recursion based on the update action defined in the fetch configuration.
        if (context.updateAction()) {
            val nextTime = startTime + context.updateIntervalMillis
            loadCache()
            processDailyUpdate(context, nextTime)
        }
    }
}
