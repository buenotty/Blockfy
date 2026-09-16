package com.buenotty.blockfy.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object DailyUsageSerializer : Serializer<DailyUsage> {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override val defaultValue: DailyUsage
        get() = DailyUsage()

    override suspend fun readFrom(input: InputStream): DailyUsage {
        return try {
            json.decodeFromString(
                deserializer = DailyUsage.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: DailyUsage, output: OutputStream) {
        output.write(
            json.encodeToString(
                serializer = DailyUsage.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}
