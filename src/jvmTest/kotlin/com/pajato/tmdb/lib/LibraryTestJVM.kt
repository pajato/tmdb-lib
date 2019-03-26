package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime.Companion.now
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@ExperimentalCoroutinesApi
class LibraryTestJVM {

    private class TestConfig(
        override val baseUrl: String = "",
        override val date: String = "03_15_2019",
        override val readTimeout: Int = 50,
        override val connectTimeout: Int = 20,
        override val updateInterval: Long = 200L,
        override val updateAction: () -> Boolean = { false }
    ) : FetchConfig

    private fun runBlockingTest(
        cycles: Long = 1L,
        baseUrl: String = "",
        date: String = "03_16_2019",
        test: suspend (config: FetchConfig, startTimes: MutableList<Long>) -> Unit
    ) {
        var counter = cycles
        val actualStartTimes = mutableListOf<Long>()
        DatasetManager.resetCache()
        runBlocking {
            val config = TestConfig(baseUrl, date) {
                val result = counter-- > 0
                if (result) actualStartTimes.add(now().unixMillisLong)
                result
            }
            test(config, actualStartTimes)
        }
    }

    @Test
    fun `when the dataset manager singleton is queried with an empty name verify an error subclass`() {
        runBlockingTest { config, _ ->
            val uut = DatasetManager.getDataset("", config)
            assertEquals(1, uut.size, """Got a empty dataset for the list named ""!""")
            assertTrue(uut[0] is TmdbError, "The result is not an error!")
        }
    }

    @Test
    fun `when the dataset manager singleton is queried with an invalid name verify an error subclass`() {
        runBlockingTest { config, _ ->
            val uut = DatasetManager.getDataset("foo", config)
            assertEquals(uut.size, 1, "Invalid size for an invalid name!")
            assertTrue(uut[0] is TmdbError, "The result is not an error!")
        }
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

        val dir = object {}.javaClass.classLoader.getResource(".") ?: URL("http://")
        assertEquals("file", dir.protocol, "Incorrect protocol!")
        runBlockingTest(baseUrl = dir.toString(), date = "03_15_2019") { config, _ ->
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

    @Test
    fun `when a connect exception is forced verify the correct behavior`() {
        runBlocking {
            val listName = "fred"
            val config = TestConfig("http://localhost/", "03_15_2019")
            val result = getEntry(listName, config)
            assertEquals(listName, result.first)
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }

    @Test
    fun `when the list name is blank verify an error result`() {
        runBlocking {
            val result = fetchLines(TmdbError::class, TestConfig())
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }

    @Test
    fun `when the base url is blank verify an error result`() {
        runBlocking {
            val result = fetchLines(Network::class, TestConfig())
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }

    @Test
    fun `test that 10 days worth of daily updates works correctly`() {
        val dir = object {}.javaClass.classLoader.getResource(".") ?: URL("http://")
        assertEquals("file", dir.protocol, "Incorrect protocol!")
        runBlockingTest(10L, dir.toString()) { config, startTimes ->
            val list = DatasetManager.getDataset("collection_ids", config)
            assertEquals(10, startTimes.size, "Wrong number of cycles executed!")
            assertEquals(1, list.size, "Wrong number of records in the list!")
            assertEquals("Collection", list[0].javaClass.simpleName)
        }
    }

}

