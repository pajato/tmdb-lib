package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTime.Companion.now
import com.soywiz.klock.hours
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal const val tmdbBlankErrorMessage = "Blank JSON argument encountered."
internal const val ANONYMOUS = "anonymous" // provided for testing/code coverage only.

/** Return a platform dependent cache entry for a given dataset. */
internal expect suspend fun getEntry(listName: String, context: FetchContext): Pair<String, MutableList<TmdbData>>

/** The TMDB dataset fetch context to differentiate production vs testing access. */
abstract class FetchContext {
    abstract val baseUrl: String
    abstract val date: String
    abstract val readTimeoutMillis: Int
    abstract val connectTimeoutMillis: Int
    abstract val updateIntervalMillis: Long
    abstract val updateAction: () -> Boolean
}

/** The platform dependent task used to refresh the cached TMDB data set for a given URL. */
internal class ContextImpl(terminationOverride: Boolean = false) : FetchContext() {
    override val baseUrl: String = "http://files.tmdb.org/p/exports/"
    override val date: String = getLastExportDate(now())
    override val readTimeoutMillis: Int = 800
    override val connectTimeoutMillis: Int = 200
    override val updateIntervalMillis: Long = 24L * 60 * 60 * 1000
    override val updateAction: () -> Boolean = { !terminationOverride }
}

/** Provide a coroutine to handle fetching the daily exported TMDB datasets. */
internal suspend fun dailyCacheRefreshTask(context: FetchContext): Map<String, List<TmdbData>> = coroutineScope {
    fun isNotTmdbError(kClass: KClass<out TmdbData>) = kClass.simpleName != TmdbError::class.simpleName

    // Asynchronously fetchList the exported TMDB data set for each TmdbData subclass.
    TmdbData::class.sealedSubclasses
        .filter { kClass -> isNotTmdbError(kClass) }
        .map { kClass -> async { fetchLines(kClass, context) } }.awaitAll().toMap()
}

/**
 * Compute the export date for a given timestamp: if the time is before 8:00am UTC, use the previous export date,
 * otherwise use today's export data.
 */
internal fun getLastExportDate(timestamp: DateTime): String =
    if (timestamp.isAfter(8)) timestamp.toTmdbFormat() else (timestamp - 24.hours).toTmdbFormat()

/** Return an error or the parsed TMDB dataset for the given TmdbData subclass. */
internal suspend fun fetchLines(subclass: KClass<out TmdbData>, context: FetchContext):
        Pair<String, MutableList<TmdbData>> {
    val listName = subclass.getListName()
    val url = listName.getUrl(context)
    return if (listName.isBlank() || url.isBlank()) getErrorEntry() else getEntry(listName, context)
}

/** Provide an error entry for testing and network errors. */
internal fun getErrorEntry(): Pair<String, MutableList<TmdbData>> =
    ANONYMOUS to mutableListOf<TmdbData>(TmdbError("Ignorable TMDB subclass!"))

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
internal fun String.getUrl(context: FetchContext) =
    if (this.isBlank() || context.baseUrl.isBlank()) "" else "${context.baseUrl}${this}_${context.date}.json.gz"

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
