package com.pajato.tmdb.lib

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val tmdbBlankErrorMessage = "Blank JSON argument encountered."

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

/** The wrapper class for TMDB dataset subclasses. */
sealed class TmdbData

/** Each TMDB subclass identifies a dataset list name and a create function. */
interface TmdbDataFactory {
    val listName: String
    fun create(json: String): TmdbData
}

/** The set of collections used in TMDB. */
@Serializable
data class Collection(val id: Int = 0, val name: String = "") : TmdbData() {
    companion object : TmdbDataFactory {
        override val listName = "collection_ids"
        override fun create(json: String): TmdbData = createFromJson(json, Collection())
        //override fun getJson():
    }
}

/** The set of keywords used in TMDB. */
@Serializable
data class Keyword(val id: Int = 0, val name: String = "") : TmdbData() {
    companion object : TmdbDataFactory {
        override val listName = "keyword_ids"
        override fun create(json: String): TmdbData = createFromJson(json, Keyword())
    }
}

/** The set of movies known to TMDB. */
@Serializable
data class Movie(
    val adult: Boolean = false,
    val id: Int = -1,
    val original_title: String = "",
    val popularity: Double = 0.0,
    val video: Boolean = false
) : TmdbData() {
    companion object : TmdbDataFactory {
        override val listName = "movie_ids"
        override fun create(json: String): TmdbData = createFromJson(json, Movie())
    }
}

/** The set of TV networks known to TMDB. */
@Serializable
data class Network(val id: Int = -1, val name: String = ""): TmdbData() {
    companion object : TmdbDataFactory {
        override val listName = "tv_network_ids"
        override fun create(json: String): TmdbData = createFromJson(json, Network())
    }
}

/** The set of people (cast, crew, etc.) known to TMDB. */
@Serializable
data class Person(
    val adult: Boolean = false,
    val id: Int = -1,
    val name: String = "",
    val popularity: Double = 0.0
) : TmdbData() {
    companion object : TmdbDataFactory {
        override val listName = "person_ids"
        override fun create(json: String): TmdbData = createFromJson(json, Person())
    }
}

/** The set of production companies known to TMDB. */
@Serializable
data class ProductionCompany(val id: Int = 0, val name: String = "") : TmdbData() {
    companion object : TmdbDataFactory {
        override val listName = "production_company_ids"
        override fun create(json: String): TmdbData = createFromJson(json, ProductionCompany())
    }
}

/** The set of TV shows known to TMDB. */
@Serializable
data class TvSeries(val id: Int = -1, val original_name: String = "", val popularity: Double = 0.0) : TmdbData() {
    companion object : TmdbDataFactory {
        override val listName = "tv_series_ids"
        override fun create(json: String): TmdbData = createFromJson(json, TvSeries())
    }
}

/** A special TMDB error class providing granular message data for errors. */
data class TmdbError(val message: String) : TmdbData()

/** An extension to access the listname given a TmdbData item. */
fun TmdbData.getListName(): String = when (this) {
    is Collection -> Collection.listName
    is Keyword -> Keyword.listName
    is Movie -> Movie.listName
    is Network -> Network.listName
    is Person -> Person.listName
    is ProductionCompany -> ProductionCompany.listName
    is TvSeries -> TvSeries.listName
    is TmdbError -> ""
}
