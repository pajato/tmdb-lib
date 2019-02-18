package com.pajato.tmdb.lib

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
