# tmdb-lib

![TMDB PoweredBy Logo](images/powered-by-tmdb.png)

## Overview

A multi-platform Kotlin library providing access to The Movie Database (tmdb) for two distinct purposes:

1. daily refreshes to seven JSON based data sets (currently comprising over two million records) that briefly describe
   1. Collections (collection_ids)
   1. Keywords (keyword_ids)
   1. Movies (movie_ids)
   1. People (person_ids)
   1. Production Companies (production_company_ids)
   1. TV Networks (network_ids)
   1. TV Series (tv_series_ids)
1. a restful interface to more details on one or more of the above items

Support will be provided in the first release for Android, iOS, CLI and the Web (JS).

## API

```kotlin
fun getDataSet(name: String): List<TmdbData>

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
```


KOH Talk Links:

Google doc link: https://docs.google.com/document/d/1_VSRV1afGchpPn4SqhkXgWPsbRshhKyPQga2U1bZgMM/edit?usp=sharing

Software Developer definition: https://www.bls.gov/ooh/computer-and-information-technology/software-developers.htm#tab-2

TMDB export data: https://developers.themoviedb.org/3/getting-started/daily-file-exports
