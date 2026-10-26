package com.buenotty.blockfy.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** What happened on one finished day. A day is clean when protection was on and was never loosened. */
@Serializable
data class DayRecord(
    val date: String,
    val blocks: Int = 0,
    val savedSeconds: Long = 0L,
    val clean: Boolean = false
)

@Serializable
data class History(val days: List<DayRecord> = emptyList())

@Suppress("BlockingMethodInNonBlockingContext")
object HistorySerializer : Serializer<History> {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override val defaultValue: History
        get() = History()

    override suspend fun readFrom(input: InputStream): History {
        return try {
            json.decodeFromString(History.serializer(), input.readBytes().decodeToString())
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: History, output: OutputStream) {
        output.write(json.encodeToString(History.serializer(), t).encodeToByteArray())
    }
}
