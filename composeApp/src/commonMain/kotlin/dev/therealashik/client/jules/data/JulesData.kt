package dev.therealashik.client.jules.data

import dev.therealashik.client.jules.api.RealJulesApi
import dev.therealashik.client.jules.cache.CacheManager
import dev.therealashik.client.jules.db.DriverFactory
import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.client.jules.model.CacheConfig
import dev.therealashik.client.jules.storage.SettingsStorage
import dev.therealashik.client.jules.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object JulesData {
    private val driver by lazy {
        DriverFactory().createDriver()
    }

    private val database: JulesDatabase by lazy {
        JulesDatabase(driver)
    }

    private val cacheScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val settingsStorage by lazy {
        SettingsStorage()
    }

    val cacheManager: CacheManager by lazy {
        CacheManager(database, CacheConfig(), cacheScope)
    }

    val themeManager: ThemeManager by lazy {
        ThemeManager(database, settingsStorage)
    }

    val repository: JulesRepository by lazy {
        JulesRepository(database, RealJulesApi, cacheManager)
    }
}
