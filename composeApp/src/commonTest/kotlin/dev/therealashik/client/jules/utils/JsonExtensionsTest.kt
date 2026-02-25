package dev.therealashik.client.jules.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonExtensionsTest {
    @Test
    fun testMapToJsonObject() {
        val map = mapOf(
            "string" to "value",
            "int" to 123,
            "boolean" to true,
            "null" to null,
            "list" to listOf(1, "two"),
            "map" to mapOf("nested" to "value")
        )

        val json = map.toJsonObject()

        assertEquals(
            JsonObject(
                mapOf(
                    "string" to JsonPrimitive("value"),
                    "int" to JsonPrimitive(123),
                    "boolean" to JsonPrimitive(true),
                    "null" to JsonNull,
                    "list" to JsonArray(listOf(JsonPrimitive(1), JsonPrimitive("two"))),
                    "map" to JsonObject(mapOf("nested" to JsonPrimitive("value")))
                )
            ),
            json
        )
    }
}
