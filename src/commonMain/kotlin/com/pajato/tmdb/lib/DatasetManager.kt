package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTime.Companion.now
import com.soywiz.klock.hours
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

const val ANONYMOUS = "anonymous"

/**
 * Compute the export date for today: if the current time is before 8:00am UTC, use the previous export date, otherwise
 * use today's export data.
 */
fun getLastExportDate(timestamp: DateTime): String =
    if (timestamp.isAfter(8)) timestamp.toTmdbFormat() else (timestamp - 24.hours).toTmdbFormat()

fun Int.toTmdbFormat() = if (this > 9) "$this" else "0$this"
fun DateTime.toTmdbFormat() = "${this.month1.toTmdbFormat()}_${this.dayOfMonth.toTmdbFormat()}_${this.yearInt}"
fun DateTime.isAfter(time: Int): Boolean = this.hours > time

/** Encapsulate the task of importing the TMDB exported data sets. */
expect suspend fun dailyExportTask(): Map<String, List<TmdbData>>

/** Return a URL which can access a TMDB export data set or a sentinel value for an invalid list name. */
fun getLinesUrl(listName: String): String =
    if (listName != ANONYMOUS) "http://files.tmdb.org/p/exports/${listName}_${getLastExportDate(now())}.json.gz" else ""

/** Parse a TMDB export data set record given the list name and the line to parse. */
fun parse(listName: String, line: String): TmdbData =
    when (listName) {
        Collection.listName -> Collection.create(line)
        Keyword.listName -> Keyword.create(line)
        Movie.listName -> Movie.create(line)
        Network.listName -> Network.create(line)
        Person.listName -> Person.create(line)
        ProductionCompany.listName -> ProductionCompany.create(line)
        TvSeries.listName -> TvSeries.create(line)
        else -> TmdbError("unsupported type: $listName.")
    }

/** Manage a global collection of TMDB data sets that are updated daily. */
object DatasetManager {
    /** Set up the map associating list names to actual lists of TMDB data (dataset) for that list name. */
    // TODO: Would be much better to extend this object from CoroutineScope to allow manage lifecycle. For now just use GlobalScope
    private val data = GlobalScope.async { dailyExportTask() }

    // TODO: Schedule a daily task to update the data map.

    /** Return a dataset for a given list name. Note that this is safe to do from the UI thread. */
    suspend fun getDataset(listName: String): List<TmdbData> = data.await()[listName]
        ?: listOf(TmdbError("Empty list or invalid list name: $listName!"))
}
