package dev.therealashik.client.jules.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.jules.sdk.model.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class JulesRepositoryPerfTest {
    @Test
    fun benchmarkTransaction() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JulesDatabase.Schema.create(driver)
        val db = JulesDatabase(driver)
        val queries = db.julesDatabaseQueries
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        // Generate a large list of sources
        val remote = (1..5000).map {
            JulesSource("source_$it", "id_$it", "Source $it")
        }

        // Warm up json
        json.encodeToString(remote.first())

        // Baseline: JSON encoding inside transaction
        val startBaseline = System.currentTimeMillis()
        queries.transaction {
            queries.deleteAllSources()
            remote.forEach { source ->
                queries.insertSource(source.name, json.encodeToString(source))
            }
        }
        val endBaseline = System.currentTimeMillis()

        // Optimized: JSON encoding outside transaction
        val startOptimized = System.currentTimeMillis()
        val encoded = remote.map { it.name to json.encodeToString(it) }
        queries.transaction {
            queries.deleteAllSources()
            encoded.forEach { (name, jsonBlob) ->
                queries.insertSource(name, jsonBlob)
            }
        }
        val endOptimized = System.currentTimeMillis()

        println("Baseline time (ms): ${endBaseline - startBaseline}")
        println("Optimized time (ms): ${endOptimized - startOptimized}")
        println("Is faster? ${endOptimized - startOptimized < endBaseline - startBaseline}")
    }
}
