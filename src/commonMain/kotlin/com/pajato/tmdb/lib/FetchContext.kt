package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime.Companion.now
import kotlinx.serialization.Serializable

// Dirty code to allow for fetch context injections.
internal object ContextManager {
    var context: FetchContext = ContextImpl()
}

/** The TMDB dataset fetch context to differentiate production vs testing access. */
internal abstract class FetchContext {
    abstract val cycles: Int
    abstract val baseUrl: String
    abstract val readTimeoutMillis: Int
    abstract val connectTimeoutMillis: Int
    abstract val updateIntervalMillis: Long
    abstract var startTime: Long
    abstract var exportDate: String

    internal var counter = 0

    internal fun updateAction(): Boolean {
        startTime += updateIntervalMillis
        if (cycles == -1) exportDate = getLastExportDate(startTime)
        counter++
        return if (cycles == -1) true else (counter < cycles)
    }
}

/** The default context for managing dataset fetches from TMDB. */
internal data class ContextImpl(
    override val cycles: Int = -1,
    override var startTime: Long = now().unixMillisLong,
    override var exportDate: String = getLastExportDate(startTime),
    override val baseUrl: String = "https://files.tmdb.org/p/exports/",
    override val readTimeoutMillis: Int = 800,
    override val connectTimeoutMillis: Int = 200,
    override val updateIntervalMillis: Long = 24L * 60 * 60 * 1000
) : FetchContext() {
    constructor(contextData: ContextData) : this(
        contextData.cycles,
        contextData.startTime,
        contextData.exportDate,
        contextData.baseUrl,
        contextData.readTimeoutMillis,
        contextData.connectTimeoutMillis,
        contextData.updateIntervalMillis
    )
}

/** Provide a fetch context persistence model. */
@Serializable internal data class ContextData(
    val cycles: Int,
    val baseUrl: String,
    val readTimeoutMillis: Int,
    val connectTimeoutMillis: Int,
    val updateIntervalMillis: Long,
    val startTime: Long,
    val exportDate: String
) {
    constructor(context: FetchContext) : this(
        context.cycles,
        context.baseUrl,
        context.readTimeoutMillis,
        context.connectTimeoutMillis,
        context.updateIntervalMillis,
        context.startTime,
        context.exportDate
    )
}

internal expect fun restoreContext(): ContextData
internal expect fun saveContext()

internal fun loadContext() = restoreContext()
internal fun storeContext() = saveContext()
