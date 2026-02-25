package dev.therealashik.client.jules.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun Map<*, *>.toJsonObject(): JsonObject {
    val content = this.entries.associate { (key, value) ->
        key.toString() to value.toJsonElement()
    }
    return JsonObject(content)
}

internal fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> this.toJsonObject()
        is List<*> -> JsonArray(this.map { it.toJsonElement() })
        is JsonElement -> this
        else -> JsonPrimitive(this.toString())
    }
}
