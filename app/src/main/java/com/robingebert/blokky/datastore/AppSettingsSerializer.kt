package com.robingebert.blokky.datastore

import kotlinx.serialization.SerializationException
import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object AppSettingsSerializer : Serializer<AppSettings> {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override val defaultValue: AppSettings
        get() = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            json.decodeFromString(
                deserializer = AppSettings.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = AppSettings.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}