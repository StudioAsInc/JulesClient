package dev.therealashik.client.jules.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val autoRefreshInterval: Int = 30
)

@Serializable
data class Account(
    val id: String,
    val name: String,
    val apiKey: String,
    val apiUrl: String = "https://jules.googleapis.com/v1alpha",
    val userPreferences: UserPreferences = UserPreferences()
)

@Serializable
data class AppSettings(
    val activeThemeId: String? = null,
    val activePreset: String = ThemePreset.MIDNIGHT.name,
    val cacheConfig: CacheConfig = CacheConfig(),
    val defaultCardCollapsed: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val activeAccountId: String? = null
) {
    fun getActiveTheme(): Theme = try {
        ThemePreset.valueOf(activePreset).theme
    } catch (e: Exception) {
        ThemePreset.MIDNIGHT.theme
    }
}
