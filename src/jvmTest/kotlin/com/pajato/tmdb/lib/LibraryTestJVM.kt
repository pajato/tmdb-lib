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

    private class TestContext(
        override val baseUrl: String = "",
        override val date: String = "03_15_2019",
        override val readTimeoutMillis: Int = 50,
        override val connectTimeoutMillis: Int = 20,
        override val updateIntervalMillis: Long = 200L,
        override val updateAction: () -> Boolean = { false }
    ) : FetchContext()

    private fun runBlockingTest(
        cycles: Long = 1L,
        baseUrl: String = "",
        date: String = "03_16_2019",
        test: suspend (context: FetchContext, startTimes: MutableList<Long>) -> Unit
    ) {
        var counter = cycles
        val actualStartTimes = mutableListOf<Long>()
        DatasetManager.resetCache()
        runBlocking {
            val context = TestContext(baseUrl, date) {
                val result = counter-- > 0
                if (result) actualStartTimes.add(now().unixMillisLong)
                result
            }
            test(context, actualStartTimes)
        }
    }

    @Test
    fun `when the dataset manager singleton is queried with an empty name verify an error subclass`() {
        runBlockingTest { context, _ ->
            val uut = DatasetManager.getDataset("", context)
            assertEquals(1, uut.size, """Got a empty dataset for the list named ""!""")
            assertTrue(uut[0] is TmdbError, "The result is not an error!")
        }
    }

    @Test
    fun `when the dataset manager singleton is queried with an invalid name verify an error subclass`() {
        runBlockingTest { context, _ ->
            val uut = DatasetManager.getDataset("foo", context)
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
        runBlockingTest(baseUrl = dir.toString(), date = "03_15_2019") { context, _ ->
            TmdbData::class.sealedSubclasses.forEach {
                val listName = it.getListName()
                if (listName == "" || listName == ANONYMOUS) return@forEach
                val uut = DatasetManager.getDataset(listName, context)
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
            val context = TestContext("http://localhost/", "03_15_2019")
            val result = getEntry(listName, context)
            assertEquals(listName, result.first)
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }

    @Test
    fun `when the list name is blank verify an error result`() {
        runBlocking {
            val result = fetchLines(TmdbError::class, TestContext())
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }

    @Test
    fun `when the base url is blank verify an error result`() {
        runBlocking {
            val result = fetchLines(Network::class, TestContext())
            assertEquals(1, result.second.size)
            assertTrue(result.second[0] is TmdbError)
        }
    }

    @Test
    fun `test that 10 days worth of daily updates works correctly`() {
        val dir = object {}.javaClass.classLoader.getResource(".") ?: URL("http://")
        assertEquals("file", dir.protocol, "Incorrect protocol!")
        runBlockingTest(10L, dir.toString()) { context, startTimes ->
            val list = DatasetManager.getDataset("collection_ids", context)
            assertEquals(10, startTimes.size, "Wrong number of cycles executed!")
            assertEquals(1, list.size, "Wrong number of records in the list!")
            assertEquals("Collection", list[0].javaClass.simpleName)
        }
    }

    @Test
    fun `test that the production fetch context can terminate`() {
        DatasetManager.resetCache()
        runBlocking {
            val context = ContextImpl(true)
            val result = DatasetManager.getDataset(Collection.listName, context)
            assertEquals(1, result.size, "Wrong size!")
            assertTrue(result[0] is TmdbError)
        }

    }

}
