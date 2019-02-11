/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime
import com.soywiz.klock.hours
import com.soywiz.klock.seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class LibraryTest {
    private fun testTmdbData(name: String) {
        val nonHomogenousCollection = "The $name collection contains inconsistent typed data!"
        val list = listOf(createDefaultFromType(name)) //fetchLines(name)
        assertTrue(list.isNotEmpty(), "Incorrect number of $name!")
        when (name) {
            Collection.listName -> assertTrue(list[0] is Collection, nonHomogenousCollection)
            Keyword.listName -> assertTrue(list[0] is Keyword, nonHomogenousCollection)
            Movie.listName -> assertTrue(list[0] is Movie, nonHomogenousCollection)
            Network.listName -> assertTrue(list[0] is Network, nonHomogenousCollection)
            Person.listName -> assertTrue(list[0] is Person, nonHomogenousCollection)
            ProductionCompany.listName -> assertTrue(list[0] is ProductionCompany, nonHomogenousCollection)
            TvSeries.listName -> assertTrue(list[0] is TvSeries, nonHomogenousCollection)
            else -> fail("Unsupported type: $name.")
        }
    }

    private fun testEmptyTmdbDataItem(item: TmdbData) {
        assertTrue(item is TmdbError, "A non-blank input did not generate an error!")
    }

    // TODO: Commented because to test suspend function you need special wrapper for testing on common code
    // TODO: See these issues about current status and known workarounds
    // TODO: https://youtrack.jetbrains.com/issue/KT-22228
    // TODO: https://youtrack.jetbrains.com/issue/KT-19813
    /*@Test fun `when the dataset manager singleton is initialized verify data is correct`() = runBlocking {
        assertTrue(DatasetManager.
        getDataset("") != null, "Got a null dataset!")
    }*/

    @Test fun `when the TMDB network data is accessed the count is greater than 0`() {
        testTmdbData("tv_network_ids")
    }

    @Test fun `when the TMDB movie data is accessed the count is greater than 0`() {
        testTmdbData("movie_ids")
    }

    @Test fun `when the TMDB tv series data is accessed the count is greater than 0`() {
        testTmdbData("tv_series_ids")
    }

    @Test fun `when the TMDB person data is accessed the count is greater than 0`() {
        testTmdbData("person_ids")
    }

    @Test fun `when the TMDB production company data is accessed the count is greater than 0`() {
        testTmdbData("production_company_ids")
    }

    @Test fun `when the TMDB collections data is accessed the count is greater than 0`() {
        testTmdbData("collection_ids")
    }

    @Test fun `when the TMDB keywords data is accessed the count is greater than 0`() {
        testTmdbData("keyword_ids")
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

    private val json = """{"id":234,"name":"fred","value":98.65, "adult":true}"""

    // TODO: Commented because to test suspend function you need special wrapper for testing on common code
    // TODO: See these issues about current status and known workarounds
    // TODO: https://youtrack.jetbrains.com/issue/KT-22228
    // TODO: https://youtrack.jetbrains.com/issue/KT-19813
    /*@Test fun `when the TmdbDatasetManager object is created with an invalid name verify an error subclass`() {
        val uut = DatasetManager.getDataset("foo")
        assertEquals(uut.size, 1, "Invalid size for an invalid name!")
    }*/

    @Test fun `when the last export date is before 8am UTC verify previous day`() {
        val timestamp1 = DateTime.fromUnix(0L)
        val timestamp2 = timestamp1 + 8.hours + 3599.seconds
        assertEquals("12_31_1969", getLastExportDate(timestamp1), "Invalid Unix big bang date format!")
        assertEquals("12_31_1969", getLastExportDate(timestamp2), "Invalid date format!")
    }

    @Test fun `when the last export date is after 8am UTC verify same day`() {
        val timestamp = DateTime.fromUnix(60 * 60 * 1000 * 9L)
        assertEquals("01_01_1970", getLastExportDate(timestamp), "Invalid date format!")
    }

    @Test fun `when the last export date is now verify same day`() {
        val timestamp = DateTime.now()
        val formattedTimestamp = getLastExportDate(timestamp)
        val linesUrl = getLinesUrl("network_ids")
        assertTrue(linesUrl.contains(formattedTimestamp), "Invalid date format!")
    }

    @Test fun `when an invalid list name is parsed verify a correct error message`() {
        assertTrue(parse("invalidListName", "") is TmdbError)
    }

    @Test fun `when an error item is passed created verify it matches the default`() {
        val errorItem = TmdbError("A default error item.")
        assertEquals(errorItem, createFromJson("{}", errorItem), "Invalid error item creation!")
    }
}