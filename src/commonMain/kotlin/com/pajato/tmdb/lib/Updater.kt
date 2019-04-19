package com.pajato.tmdb.lib

expect object Updater {
    internal val datasetCache: MutableMap<String, Dataset>
    fun getDataset(listName: String): Dataset
}
