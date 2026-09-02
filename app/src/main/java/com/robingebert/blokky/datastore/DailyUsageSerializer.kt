package com.robingebert.blokky.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object DailyUsageSerializer : Serializer<DailyUsage> {

    override val defaultValue: DailyUsage
        get() = DailyUsage()

    override suspend fun readFrom(input: InputStream): DailyUsage {
        return try {
            Json.decodeFromString(
                deserializer = DailyUsage.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: DailyUsage, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = DailyUsage.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}
