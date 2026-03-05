package dev.therealashik.client.jules.storage

/**
 * Represents a single migration step from one version to another.
 *
 * @param fromVersion The version to migrate from.
 * @param toVersion The version this migration upgrades to.
 * @param migrate The actual migration logic that applies changes to [SettingsStorage].
 */
class Migration(
    val fromVersion: Long,
    val toVersion: Long,
    val migrate: suspend (SettingsStorage) -> Unit
)

/**
 * Responsible for applying a series of [Migration]s to a [SettingsStorage] instance
 * sequentially to reach a target version.
 */
class SettingsMigrator(private val storage: SettingsStorage) {

    companion object {
        const val VERSION_KEY = "settings_version"
    }

    /**
     * Executes the given [migrations] sequentially to update the current version
     * of the storage to the [targetVersion].
     *
     * @param targetVersion The final version the storage should reach.
     * @param migrations The list of available migrations.
     * @return true if all migrations were successful, false if an error occurred.
     */
    suspend fun migrate(targetVersion: Long, migrations: List<Migration>): Boolean {
        var currentVersion = storage.getLong(VERSION_KEY, 0L)

        if (currentVersion >= targetVersion) {
            return true
        }

        val sortedMigrations = migrations.sortedBy { it.fromVersion }

        for (migration in sortedMigrations) {
            if (migration.fromVersion == currentVersion && migration.toVersion <= targetVersion) {
                try {
                    migration.migrate(storage)
                    currentVersion = migration.toVersion
                    storage.saveLong(VERSION_KEY, currentVersion)
                } catch (e: Exception) {
                    // Log error here if logging is available
                    return false
                }
            }

            if (currentVersion == targetVersion) {
                break
            }
        }

        return currentVersion == targetVersion
    }
}
