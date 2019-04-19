package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime
import com.soywiz.klock.hours
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal const val tmdbBlankErrorMessage = "Blank JSON argument encountered."
internal const val ANONYMOUS = "anonymous" // provided for testing/code coverage only.

expect class Dataset(listName: String,  path: String = "", length: Long = -1L,  error: String = "") {
    val listName: String
    val path: String
    val length: Long
    val error: String
    fun getFirstPage(): List<TmdbData>
}

internal expect suspend fun getCacheEntry(listName: String, url: String): Pair<String, Dataset>

/** Provide a suspendable function to handle fetching the daily exported TMDB datasets. */
internal suspend fun cacheUpdateTask(): Map<String, Dataset> = coroutineScope {
    fun isNotTmdbError(kClass: KClass<out TmdbData>) = kClass.simpleName != TmdbError::class.simpleName

    // Asynchronously fetch the exported TMDB data set for each TmdbData subclass, excluding TmdbError.
    TmdbData::class.sealedSubclasses
        .filter { kClass -> isNotTmdbError(kClass) }
        .map { kClass -> async { fetchLines(kClass) } }.awaitAll().toMap()
}

/**
 * Compute the export date for a given timestamp: if the time is before 8:00am UTC, use the previous export date,
 * otherwise use today's export data.
 */
internal fun getLastExportDate(timestamp: Long): String {
    val date = DateTime(timestamp)
    return if (date.isAfter(8)) date.toTmdbFormat() else (date - 24.hours).toTmdbFormat()
}

/** Return an error or the parsed TMDB dataset for the given TmdbData subclass. */
internal suspend fun fetchLines(subclass: KClass<out TmdbData>): Pair<String, Dataset> {
    fun getErrorEntry(listName: String): Pair<String, Dataset> =
        listName to Dataset(listName, "", -1L,  "Ignorable TMDB subclass or invalid date directory!")
    val listName = subclass.getListName()
    val url = listName.getUrl()

    return if (listName.isBlank() || url.isBlank()) getErrorEntry(listName) else getCacheEntry(listName, url)
}

/** Provide an error entry for testing and network errors. */

/** Parse a TMDB export data set record given the list name and the line to parse. */
internal fun parse(listName: String, line: String): TmdbData =
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
internal fun createDefaultFromType(type: String): TmdbData = when (type) {
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
internal fun createFromJson(json: String, item: TmdbData): TmdbData =
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

internal fun Int.toTmdbFormat() = if (this > 9) "$this" else "0$this"
internal fun DateTime.toTmdbFormat() = "${this.month1.toTmdbFormat()}_${this.dayOfMonth.toTmdbFormat()}_${this.yearInt}"
internal fun DateTime.isAfter(time: Int): Boolean = this.hours > time

/** Return the empty string or a URL with a valid TMDB dataset path. */
internal fun String.getUrl(): String {
    val baseUrl = ContextManager.context.baseUrl
    val exportDate = ContextManager.context.exportDate
    return if (this.isBlank() || baseUrl.isBlank()) "" else "$baseUrl${this}_$exportDate.json.gz"
}

/** An extension to access the list name given a TmdbData item. */
internal fun TmdbData.getListName(): String = when (this) {
    is Collection -> Collection.listName
    is Keyword -> Keyword.listName
    is Movie -> Movie.listName
    is Network -> Network.listName
    is Person -> Person.listName
    is ProductionCompany -> ProductionCompany.listName
    is TvSeries -> TvSeries.listName
    is TmdbError -> ""
}

/** An extensions to return a TMDB export data set list name for a given subclass. */
internal fun KClass<out TmdbData>.getListName(): String {
    val name = this.simpleName ?: return ANONYMOUS
    return createDefaultFromType(name).getListName()
}
