package dev.therealashik.client.jules.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.client.jules.model.CacheConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class CacheManagerTest {

    @Test
    fun testClearByPrefix() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JulesDatabase.Schema.create(driver)
        val database = JulesDatabase(driver)
        val cacheManager = CacheManager(database, CacheConfig(), kotlinx.coroutines.GlobalScope)

        // Set up initial data
        cacheManager.set("user_1", "value1")
        cacheManager.set("user_2", "value2")
        cacheManager.set("settings_1", "value3")
        cacheManager.set("other", "value4")

        // Verify data is there
        assertEquals("value1", cacheManager.get("user_1"))
        assertEquals("value2", cacheManager.get("user_2"))
        assertEquals("value3", cacheManager.get("settings_1"))
        assertEquals("value4", cacheManager.get("other"))

        // Clear by prefix
        cacheManager.clearByPrefix("user_")

        // Verify prefix keys are gone
        assertNull(cacheManager.get("user_1"))
        assertNull(cacheManager.get("user_2"))

        // Verify other keys remain
        assertEquals("value3", cacheManager.get("settings_1"))
        assertEquals("value4", cacheManager.get("other"))

        // Verify stats are updated
        assertEquals(2, cacheManager.stats.value.entryCount)
    }
}
