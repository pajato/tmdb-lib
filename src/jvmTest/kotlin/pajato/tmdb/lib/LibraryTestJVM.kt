package com.pajato.tmdb.lib

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue

class LibraryTestJVM {
    @Test
    fun `when the dataset manager singleton is initialized verify data is correct`() = runBlocking {
        assertTrue(DatasetManager.getDataset("") != null, "Got a null dataset!")
    }
}
