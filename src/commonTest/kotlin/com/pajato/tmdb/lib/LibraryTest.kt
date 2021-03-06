/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTime.Companion.now
import com.soywiz.klock.hours
import com.soywiz.klock.seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class LibraryTest {
    private fun testTmdbData(name: String) {
        val nonHomogenousCollection = "The $name collection contains inconsistent typed data!"
        val list = listOf(createDefaultFromType(name))
        assertTrue(list.size == 1 && list[0] !is TmdbError, "Detected an error in the $name default creation.")
        assertTrue(list.isNotEmpty(), "Incorrect number of $name!")
        when (name) {
            "Collection" -> assertTrue(list[0] is Collection, nonHomogenousCollection)
            "Keyword" -> assertTrue(list[0] is Keyword, nonHomogenousCollection)
            "Movie" -> assertTrue(list[0] is Movie, nonHomogenousCollection)
            "Network" -> assertTrue(list[0] is Network, nonHomogenousCollection)
            "Person" -> assertTrue(list[0] is Person, nonHomogenousCollection)
            "ProductionCompany" -> assertTrue(list[0] is ProductionCompany, nonHomogenousCollection)
            "TvSeries" -> assertTrue(list[0] is TvSeries, nonHomogenousCollection)
            else -> fail("Unsupported type: $name.")
        }
    }

    private fun testEmptyTmdbDataItem(item: TmdbData) {
        assertTrue(item is TmdbError, "A non-blank input did not generate an error!")
    }

    @Test fun `when the TMDB network data is accessed the count is greater than 0`() {
        testTmdbData("Network")
    }

    @Test fun `when the TMDB movie data is accessed the count is greater than 0`() {
        testTmdbData("Movie")
    }

    @Test fun `when the TMDB tv series data is accessed the count is greater than 0`() {
        testTmdbData("TvSeries")
    }

    @Test fun `when the TMDB person data is accessed the count is greater than 0`() {
        testTmdbData("Person")
    }

    @Test fun `when the TMDB production company data is accessed the count is greater than 0`() {
        testTmdbData("ProductionCompany")
    }

    @Test fun `when the TMDB collections data is accessed the count is greater than 0`() {
        testTmdbData("Collection")
    }

    @Test fun `when the TMDB keywords data is accessed the count is greater than 0`() {
        testTmdbData("Keyword")
    }

    @Test fun `when creating a collection with a blank JSON argument that an error is signalled`() {
        testEmptyTmdbDataItem(Collection.create(""))
    }

    @Test fun `when creating a keyword with a blank JSON argument that an error is signalled`() {
        testEmptyTmdbDataItem(Keyword.create(""))
    }

    @Test fun `when creating a movie with a blank JSON argument that an error is signalled`() {
        testEmptyTmdbDataItem(Movie.create(""))
    }

    @Test fun `when creating a network with a blank JSON argument that an error is signalled`() {
        testEmptyTmdbDataItem(Network.create(""))
    }

    @Test fun `when creating a person with a blank JSON argument that an error is signalled`() {
        testEmptyTmdbDataItem(Person.create(""))
    }

    @Test fun `when creating a production company with a blank JSON argument that an error is signalled`() {
        testEmptyTmdbDataItem(ProductionCompany.create(""))
    }

    @Test fun `when creating a tv series with a blank JSON argument that an error is signalled`() {
        testEmptyTmdbDataItem(TvSeries.create(""))
    }


    @Test fun `when the last export date is before 8am UTC verify previous day`() {
        val timestamp1 = DateTime.fromUnix(0L)
        val timestamp2 = timestamp1 + 8.hours + 3599.seconds
        assertEquals("12_31_1969", getLastExportDate(timestamp1.unixMillisLong), "Invalid Unix date format!")
        assertEquals("12_31_1969", getLastExportDate(timestamp2.unixMillisLong), "Invalid date format!")
    }

    @Test fun `when the last export date is after 8am UTC verify same day`() {
        val timestamp = DateTime.fromUnix(60 * 60 * 1000 * 9L)
        assertEquals("01_01_1970", getLastExportDate(timestamp.unixMillisLong), "Invalid date format!")
    }

    @Test fun `when an invalid list name is parsed verify a correct error message`() {
        assertTrue(parse("invalidListName", "") is TmdbError)
    }

    @Test fun `when an error item is passed created verify it matches the default`() {
        val errorItem = TmdbError("A default error item.")
        assertEquals(errorItem, createFromJson("{}", errorItem), "Invalid error item creation!")
    }

    @Test fun `when the data access context object is defaulted verify correct results`() {
        val context = ContextImpl()
        assertEquals("https://files.tmdb.org/p/exports/", context.baseUrl, "Base URL error in context!")
        assertEquals(10, context.exportDate.length, "Date error in context!")
    }

    @Test fun `when an invalid list name is parsed verify an error result`() {
        val result = parse("", "")
        assertTrue(result is TmdbError, "Parsing error detection failed!")
    }

    @Test
    fun `verify that the fetch context update action works correctly`() {
        val uutWithoutOverride = ContextImpl(cycles = -1)
        assertTrue(uutWithoutOverride.updateAction(), "With no override gives wrong result!")
        val uutWithOverride = ContextImpl(cycles = 1)
        assertFalse(uutWithOverride.updateAction(), "With override gives wrong result!")
    }

    @Test fun `exercise the parser for each TmdbData subclass`() {
        parse(Collection.listName, """{"id":645,"name":"James Bond Collection"}""")
        parse(Keyword.listName, """{"id":730,"name":"factory worker"}""")
        parse(Movie.listName, """{"adult":false,"id":603,"original_title":"The Matrix","popularity":32.156,"video":false}""")
        parse(Person.listName, """{"adult":false,"id":658,"name":"Alfred Molina","popularity":4.154}""")
        parse(ProductionCompany.listName, """{"id":601,"name":"Blake Edwards Entertainment"}""")
        parse(Network.listName, """{"id":601,"name":"ABC News"}""")
        parse(TvSeries.listName, """{"id":602,"original_name":"Love on a Rooftop","popularity":1.133}""")
    }

    @Test fun `test context persistence`() {
        val cycles = 10
        val startTime = now().unixMillisLong
        val baseUrl = "httpz://somestuff"
        val exportDate = "03_10_2019"
        val readTimeout = 55
        val connectTimeout = 125
        val updateInterval = 250L
        val expectedContext = ContextImpl(
            cycles,
            startTime,
            exportDate,
            baseUrl,
            readTimeout,
            connectTimeout,
            updateInterval
        )
        ContextManager.context = expectedContext
        storeContext()
        val actualContext = ContextImpl(loadContext())
        assertEquals(ContextData(expectedContext), ContextData(actualContext), "Contexts are not the same!")
    }

    @Test fun `when a dataset is created test the properties`() {
        val listName = Collection.listName
        val path = "file://foo"
        val length = 12L
        val errorMessage = "some problem exists"
        val uut = Dataset(listName, path, length, errorMessage)
        assertEquals(listName, uut.listName, "List name is wrong!")
        assertEquals(path, uut.path, "Path is wrong!")
        assertEquals(length, uut.length, "Length name is wrong!")
        assertEquals(errorMessage, uut.error, "Error message name is wrong!")
    }
}
