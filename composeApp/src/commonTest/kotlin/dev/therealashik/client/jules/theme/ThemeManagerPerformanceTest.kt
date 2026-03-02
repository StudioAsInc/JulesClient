package dev.therealashik.client.jules.theme

import kotlin.test.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dev.therealashik.client.jules.model.Theme
import kotlin.time.measureTime

class ThemeManagerPerformanceTest {

    @Test
    fun benchmarkThemeDecoding() {
        val json = Json { ignoreUnknownKeys = true }
        val dummyTheme = Theme("#000000", "#111111", "#222222", "#333333", "#4444ff", "#ffffff", "#aaaaaa")
        val themeJson = json.encodeToString(dummyTheme)

        // Let's say we have 1000 themes to decode (baseline)
        val jsons = List(1000) { themeJson }

        val timeToDecodeAll = measureTime {
            val themes = jsons.map { json.decodeFromString<Theme>(it) }
        }

        // With optimization, we don't decode them again if cached.
        // Caching 1000 items from a list instead of decoding
        // In the optimized case, we just map over them and do a lookup
        val decodedTheme = json.decodeFromString<Theme>(themeJson)
        val timeToCacheLookupAll = measureTime {
            val themes = jsons.map { decodedTheme }
        }

        println("Baseline - Decoding 1000 themes took: $timeToDecodeAll")
        println("Optimized - Lookup 1000 themes took: $timeToCacheLookupAll")
    }
}
