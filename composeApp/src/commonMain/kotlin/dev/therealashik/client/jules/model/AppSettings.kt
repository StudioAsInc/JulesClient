package dev.therealashik.client.jules.model

import kotlinx.serialization.Serializable

// TODO: Add user preferences (language, notifications, auto-refresh intervals)
@Serializable
data class Account(
    val id: String,
    val name: String,
    val apiKey: String,
    val apiUrl: String = "https://jules.googleapis.com/v1alpha"
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
