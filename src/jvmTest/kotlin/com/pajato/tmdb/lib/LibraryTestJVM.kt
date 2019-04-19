package com.pajato.tmdb.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class LibraryTestJVM {

    @get:Rule val testName = TestName()

    private val testResourceDir by lazy { Updater.getResourceDir("test") }
    private val testDatasetsDir = File(testResourceDir, DATASETS)

    @BeforeTest fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        assertTrue(Updater.datasetCache.isEmpty(), "The dataset cache is not empty!")
        assertEquals(8, File(Updater.mainDatasetsDir.path).listFiles().size, "Wrong number of files!")
    }

    @AfterTest fun tearDown() {
        // Re-install datasets content by deleting and copying from test resources.

        // Step 1: delete the datasets created by previous tests, if any.
        Updater.deleteLoadedDatasets(Updater.mainDatasetsDir)
        assertEquals(0, Updater.mainDatasetsDir.listFiles().size, "Datasets directory is not empty!")
        Updater.deleteLoadedDatasets(Updater.mainStagingDir)
        assertEquals(0, Updater.mainStagingDir.listFiles().size, "Staging directory is not empty!")

        // Step 2: copy the default datasets for the test resources.
        this.testDatasetsDir.walk().iterator().forEach {
            if (it.isFile && !it.isDirectory) it.copyTo(File(Updater.mainDatasetsDir, it.name), true)
        }
        assertEquals(8, File(Updater.mainDatasetsDir.path).listFiles().size, "Wrong number of files!")

        // Step 3: ensure the cache is cleared.
        Updater.datasetCache.clear()
    }

    private fun runBlockingTest(
        cycles: Int = 1,
        baseUrl: String = "",
        date: String = "03_16_2019",
        test: suspend (context: FetchContext) -> Unit
    ) {
        runBlocking {
            val context = ContextImpl(
                cycles = cycles,
                baseUrl = baseUrl,
                exportDate = date,
                readTimeoutMillis = 50,
                connectTimeoutMillis = 20,
                updateIntervalMillis = 200L)
            test(context)
        }
    }

    @Test
    fun `when the test server is pinged with cached dataset names verify no errors`() {
        val dir = object {}.javaClass.classLoader.getResource(".") ?: URL("http://")
        assertEquals("file", dir.protocol, "Incorrect protocol!")
        runBlockingTest(cycles = 0, baseUrl = dir.toString(), date = "03_16_2019") { context ->
            ContextManager.context = context
            TmdbData::class.sealedSubclasses.forEach {
                val listName = it.getListName()
                if (listName == "" || listName == ANONYMOUS) return@forEach
                val uut = Updater.getDataset(listName)
                assertTrue(uut.error.isEmpty(), "List with name '$listName' has error: ${uut.error}")
            }
            delay(7 * context.updateIntervalMillis)
        }
    }

    @Test
    fun `when a connect exception is forced verify the correct behavior`() {
        runBlockingTest(cycles = 1, baseUrl = "http://localhost/", date = "03_16_2019") { context ->
            val listName = Collection.listName
            ContextManager.context = context
            val url = listName.getUrl()
            val result = getCacheEntry(listName, url)
            assertTrue(result.second.error.isNotEmpty(), "Exception did not happen!")
        }
    }

    @Test
    fun `when a file not found exception is forced verify the correct behavior`() {
        runBlockingTest(cycles = 1, baseUrl = "file:///", date = "03_16_2019") { context ->
            val listName = Collection.listName
            ContextManager.context = context
            val url = listName.getUrl()
            val result = getCacheEntry(listName, url)
            assertTrue(result.second.error.isNotEmpty(), "Exception did not happen!")
        }
    }

    @Test
    fun `when the list name is blank while fetching lines verify an error result`() {
        runBlockingTest(cycles = 1, baseUrl = "file:///", date = "03_16_2019") { context ->
            ContextManager.context = context
            val result = fetchLines(TmdbError::class)
            delay(2 * context.updateIntervalMillis)
            assertEquals(-1, result.second.length)
            assertTrue(result.second.error.isNotEmpty())
        }
    }

    @Test
    fun `when the base url is blank while fetching lines verify an error result`() {
        runBlockingTest(cycles = 1, baseUrl = "", date = "03_16_2019") { context ->
            ContextManager.context = context
            val result = fetchLines(Network::class)
            delay(2 * context.updateIntervalMillis)
            assertEquals(-1, result.second.length)
            assertTrue(result.second.error.isNotEmpty())
        }
    }

    @Test
    fun `test that 2 cycles worth of updates works correctly`() {
        val cycleCount = 2
        val dir = object {}.javaClass.classLoader.getResource(".") ?: URL("http://")
        assertEquals("file", dir.protocol, "Incorrect protocol!")
        runBlockingTest(cycles = 2, baseUrl = dir.toString(), date = "03_16_2019") { context ->
            ContextManager.context = context
            val uut = Updater.getDataset("collection_ids")
            delay(8 * context.updateIntervalMillis)
            assertEquals(cycleCount, context.counter, "Wrong number of cycles executed!")
            assertTrue(uut.error.isEmpty(), "An error occurred: ${uut.error}")
            assertEquals(Collection.listName, uut.listName, "Invalid list type!")
        }
    }

    @Test
    fun `when a dataset with an invalid name is accessed verify an error is generated`() {
        runBlockingTest(cycles = 1, date = "03_16_2019") { context ->
            ContextManager.context = context
            val uut = Updater.getDataset("foo")
            delay(2 * context.updateIntervalMillis)
            assertTrue(uut.error.isNotEmpty(), "The result is not an error!")
        }
    }

    @Test
    fun `when a dataset is accessed provide a chunk of data`() {
        fun getErrorMessage(list: List<TmdbData>) =
            "The page (${list.size}) for date: {${ContextManager.context.exportDate}} is not between 0 and 25 records!"

        ContextManager.context = ContextImpl(cycles = 0)
        val uut = Updater.getDataset(Movie.listName)
        val list = uut.getFirstPage()
        assertTrue(list.size in 0..24, getErrorMessage(list))
    }

    @Test
    fun `no operation`() {
        // ensure that the startup state is in place.
        assertTrue(Updater.mainResourceDir.isNotEmpty(), "The main resource directory is not available!")
        assertTrue(testResourceDir.isNotEmpty(), "The test resource directory is not available!")
        assertEquals(8, Updater.mainDatasetsDir.listFiles().size, "Missing some files!")
    }
}
