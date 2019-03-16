package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTime.Companion.now
import com.soywiz.klock.hours
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

const val tmdbBlankErrorMessage = "Blank JSON argument encountered."
const val ANONYMOUS = "anonymous" // provided for testing/code coverage only.

/** The TMDB dataset fetch configuration to differentiate production (defaults) vs testing access. */
class FetchConfig(
    val baseUrl: String = "http://files.tmdb.org/p/exports/",
    val date: String = getLastExportDate(now()),
    val readTimeout: Int = 800,
    val connectTimeout: Int = 200
) {
    fun getUrl(listName: String) = if (baseUrl.isBlank()) "" else "$baseUrl${listName}_$date.json.gz"
}

/** The platform dependent task used to refresh the cached TMDB data set for a given URL. */
expect suspend fun getEntry(listName: String, config: FetchConfig): Pair<String, MutableList<TmdbData>>

/** Provide a coroutine to handle fetching the daily exported TMDB datasets. */
suspend fun dailyCacheRefreshTask(config: FetchConfig): Map<String, List<TmdbData>> = coroutineScope {
    fun isNotTmdbError(kClass: KClass<out TmdbData>) = kClass.simpleName != TmdbError::class.simpleName

    // Asynchronously fetchList the exported TMDB data set for each TmdbData subclass.
    TmdbData::class.sealedSubclasses
        .filter { kClass -> isNotTmdbError(kClass) }
        .map { kClass -> async { fetchLines(kClass, config) } }.awaitAll().toMap()
}

/**
 * Compute the export date for a given timestamp: if the time is before 8:00am UTC, use the previous export date,
 * otherwise use today's export data.
 */
fun getLastExportDate(timestamp: DateTime): String =
    if (timestamp.isAfter(8)) timestamp.toTmdbFormat() else (timestamp - 24.hours).toTmdbFormat()

/** Fetch and parse the export data set records for the given TmdbData subclass. */
suspend fun fetchLines(subclass: KClass<out TmdbData>, config: FetchConfig): Pair<String, MutableList<TmdbData>> {
    val listName = subclass.getListName()
    val url = config.getUrl(listName)
    return if (listName.isBlank() || url.isBlank()) getErrorEntry() else getEntry(listName, config)
}

/** Provide an error entry for testing and network errors. */
fun getErrorEntry(): Pair<String, MutableList<TmdbData>> =
    ANONYMOUS to mutableListOf<TmdbData>(TmdbError("Ignorable TMDB subclass!"))

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
        else -> TmdbError("Unsupported type: $listName.")
    }

/** Return a TMDB subclass given a type string. */
fun createDefaultFromType(type: String): TmdbData = when (type) {
    "Collection" -> Collection()
    "Keyword" -> Keyword()
    "Movie" -> Movie()
    "Network" -> Network()
    "Person" -> Person()
    "ProductionCompany" -> ProductionCompany()
    "TvSeries" -> TvSeries()
    else -> TmdbError("Attempt to create an invalid TMDB data item!")
}

/** Return a TMDB subclass for a given TMDB default data item and a JSON spec. */
fun createFromJson(json: String, item: TmdbData): TmdbData =
    if (json.isBlank()) TmdbError(tmdbBlankErrorMessage) else when (item) {
        is Collection -> Json.parse(Collection.serializer(), json)
        is Keyword -> Json.parse(Keyword.serializer(), json)
        is Network -> Json.parse(Network.serializer(), json)
        is ProductionCompany -> Json.parse(ProductionCompany.serializer(), json)
        is Movie -> Json.parse(Movie.serializer(), json)
        is Person -> Json.parse(Person.serializer(), json)
        is TvSeries -> Json.parse(TvSeries.serializer(), json)
        is TmdbError -> item
    }

// Extension functions

fun Int.toTmdbFormat() = if (this > 9) "$this" else "0$this"
fun DateTime.toTmdbFormat() = "${this.month1.toTmdbFormat()}_${this.dayOfMonth.toTmdbFormat()}_${this.yearInt}"
fun DateTime.isAfter(time: Int): Boolean = this.hours > time

/** An extensions to return a TMDB export data set list name for a given subclass. */
fun KClass<out TmdbData>.getListName(): String {
    val name = this.simpleName ?: return ANONYMOUS
    return createDefaultFromType(name).getListName()
}
