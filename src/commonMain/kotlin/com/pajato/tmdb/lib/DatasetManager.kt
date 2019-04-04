package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime.Companion.now
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Manage a global cache of TMDB data sets that are updated daily. */
@ExperimentalCoroutinesApi
object DatasetManager {
    internal val datasetCache = mutableMapOf<String, List<TmdbData>>()

    /** Return a dataset for a given list name. Note that this is safe to do from the UI/Main thread. */
    suspend fun getDataset(listName: String, context: FetchContext = ContextImpl()): List<TmdbData> {
        if (datasetCache.isEmpty()) processCacheUpdate(context, now().unixMillisLong)
        return datasetCache[listName] ?: listOf(TmdbError("..."))
    }

    internal suspend fun processCacheUpdate(context: FetchContext, startTime: Long) {
        suspend fun updateCache(data: Deferred<Map<String, List<TmdbData>>>) {
            data.await()
            for (entry in data.getCompleted().entries) {
                if (entry.key == ANONYMOUS) continue
                datasetCache[entry.key] = entry.value
            }
        }

        coroutineScope { updateCache(async { dailyCacheRefreshTask(context) }) }
        scheduleNextUpdate(context, startTime + context.updateIntervalMillis)
    }
}
