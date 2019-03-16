package com.pajato.tmdb.lib

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@ExperimentalCoroutinesApi
class LibraryTestJVM {

    @Test
    fun `when the dataset manager singleton is queried with an empty name verify an error subclass`() =
        runBlocking {
            val uut = DatasetManager.getDataset("")
            assertEquals(1, uut.size, """Got a empty dataset for the list named ""!""")
            assertTrue(uut[0] is TmdbError, "The result is not an error!")
        }

    @Test
    fun `when the dataset manager singleton is queried with an invalid name verify an error subclass`() =
        runBlocking {
            val uut = DatasetManager.getDataset("foo")
            assertEquals(uut.size, 1, "Invalid size for an invalid name!")
            assertTrue(uut[0] is TmdbError, "The result is not an error!")
        }

    @Test
    fun `when the test server is pinged with valid names verify correct list sizes`() {
        fun failWithNoErrorOrData() { fail("No error or data returned!") }
        fun failWithError(error: TmdbError) { fail("An error occurred: ${error.message}") }
        fun getExpectedSize(listName: String) = when (listName) {
            Collection.listName -> 3081
            Keyword.listName -> 36841
            Movie.listName -> 447414
            Network.listName -> 1873
            Person.listName -> 1329226
            ProductionCompany.listName -> 85018
            TvSeries.listName -> 80052
            else -> -1
        }

        runBlocking {
            val dir = object {}.javaClass.classLoader.getResource(".") ?: URL("http://")
            assertEquals("file", dir.protocol, "Incorrect protocol!")
            val config = FetchConfig(dir.toString(), "03_15_2019", 50, 20)
            TmdbData::class.sealedSubclasses.forEach {
                val listName = it.getListName()
                if (listName == "" || listName == ANONYMOUS) return@forEach
                val uut = DatasetManager.getDataset(listName, config)
                when {
                    uut.isEmpty() -> failWithNoErrorOrData()
                    uut[0] is TmdbError -> failWithError(uut[0] as TmdbError)
                    else -> assertEquals(getExpectedSize(listName), uut.size, "Data set list $listName size is wrong!")
                }
            }
        }
    }

    @Test fun `when a connect exception is forced verify the correct behavior`() {
        runBlocking {
            val listName = "fred"
            val config = FetchConfig("http://localhost/", "03_15_2019")
            val result = getEntry(listName, config)
            assertEquals(listName, result.first)
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }

    @Test fun `when the list name is forced to be a blank verify an error result`() {
        runBlocking {
            val result = fetchLines(TmdbError::class, FetchConfig())
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }

    @Test fun `when the url is forced to be a blank verify an error result`() {
        runBlocking {
            val result = fetchLines(Network::class, FetchConfig(""))
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }
}

