package dev.therealashik.client.jules.storage

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsMigratorTest {

    private lateinit var storage: SettingsStorage
    private lateinit var migrator: SettingsMigrator

    @BeforeTest
    fun setup() = runTest {
        storage = SettingsStorage()
        storage.clear() // Ensure a clean state for each test
        migrator = SettingsMigrator(storage)
    }

    @Test
    fun testSuccessfulSequentialMigrations() = runTest {
        // Initial state setup
        storage.saveLong(SettingsMigrator.VERSION_KEY, 0L)
        storage.saveString("old_key_1", "value1")

        val migrations = listOf(
            Migration(0L, 1L) { store ->
                val v1 = store.getString("old_key_1", "")
                store.saveString("new_key_1", v1)
                store.delete("old_key_1")
            },
            Migration(1L, 2L) { store ->
                store.saveBoolean("new_feature_enabled", true)
            }
        )

        val result = migrator.migrate(2L, migrations)

        assertTrue(result, "Migration should succeed")
        assertEquals(2L, storage.getLong(SettingsMigrator.VERSION_KEY, -1L))
        assertEquals("value1", storage.getString("new_key_1", ""))
        assertEquals("", storage.getString("old_key_1", ""))
        assertTrue(storage.getBoolean("new_feature_enabled", false))
    }

    @Test
    fun testMigrationSkipsIfAlreadyAtTargetVersion() = runTest {
        storage.saveLong(SettingsMigrator.VERSION_KEY, 2L)

        var migrationRun = false
        val migrations = listOf(
            Migration(0L, 1L) {
                migrationRun = true
            },
            Migration(1L, 2L) {
                migrationRun = true
            }
        )

        val result = migrator.migrate(2L, migrations)

        assertTrue(result, "Migration should report success immediately")
        assertEquals(2L, storage.getLong(SettingsMigrator.VERSION_KEY, -1L))
        assertFalse(migrationRun, "No migrations should be executed")
    }

    @Test
    fun testMigrationFailsAndHaltsOnError() = runTest {
        storage.saveLong(SettingsMigrator.VERSION_KEY, 0L)

        var step3Run = false
        val migrations = listOf(
            Migration(0L, 1L) { store ->
                store.saveString("step1", "done")
            },
            Migration(1L, 2L) {
                throw Exception("Failed during migration step 2")
            },
            Migration(2L, 3L) {
                step3Run = true
            }
        )

        val result = migrator.migrate(3L, migrations)

        assertFalse(result, "Migration should report failure")
        assertEquals(1L, storage.getLong(SettingsMigrator.VERSION_KEY, -1L), "Version should only reach 1")
        assertEquals("done", storage.getString("step1", ""))
        assertFalse(step3Run, "Step 3 should not be executed")
    }

    @Test
    fun testGapInMigrationsFailsToReachTarget() = runTest {
        storage.saveLong(SettingsMigrator.VERSION_KEY, 0L)

        // Missing migration from 1L to 2L
        val migrations = listOf(
            Migration(0L, 1L) { },
            Migration(2L, 3L) { }
        )

        val result = migrator.migrate(3L, migrations)

        assertFalse(result, "Migration should fail to reach target because of gap")
        assertEquals(1L, storage.getLong(SettingsMigrator.VERSION_KEY, -1L))
    }
}
