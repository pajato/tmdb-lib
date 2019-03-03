package com.pajato.tmdb.lib

//import io.ktor.server.jetty.Jetty
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class LibraryTestJVM {
    companion object {
        val args: Array<String> = arrayOf("-port=8083")
        //val server = embeddedServer(Jetty, commandLineEnvironment(args))
        @BeforeClass @JvmStatic fun setup() {
            //server.start(true)
            println("Server is started.")
        }
        @AfterClass @JvmStatic fun tearDown() {
            //server.stop(0,0, TimeUnit.MILLISECONDS)
            println("Server is stopped.")
        }
    }

    private val localDatasetServerUrl = "http://localhost:8087/p/exports"

    @Test
    fun `when the local server is pinged verify a success response`() {
        //Url("http:localhost:8080").toURI().toURL().openConnection().
        Thread.sleep(2 * 1000L)
    }

    @Test
    fun `when the dataset manager singleton is queried with an empty name verify an error subclass`() =
        runBlocking {
            val uut = DatasetManager.getDataset("", localDatasetServerUrl)
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
    fun `when the dataset manager singleton is queried with an all valid names verify correct results`() =
        runBlocking {
            TmdbData::class.sealedSubclasses.forEach {
                val listName = it.getListName()
                if (listName == "" || listName == ANONYMOUS) return@forEach
                val uut = DatasetManager.getDataset(listName)
                assertTrue(uut.size > 1, "Invalid size for list named $listName!")
                println("Data set list $listName contains ${uut.size} records.")
            }
        }
}
