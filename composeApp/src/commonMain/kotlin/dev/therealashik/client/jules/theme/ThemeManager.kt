package dev.therealashik.client.jules.theme

import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.client.jules.model.CustomTheme
import dev.therealashik.client.jules.model.Theme
import dev.therealashik.client.jules.model.ThemePreset
import dev.therealashik.client.jules.storage.SettingsStorage
import dev.therealashik.client.jules.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ThemeManager(
    private val db: JulesDatabase,
    private val storage: SettingsStorage
) {
    private val queries = db.julesDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }

    private val _activeTheme = MutableStateFlow(ThemePreset.MIDNIGHT.theme)
    val activeTheme: StateFlow<Theme> = _activeTheme.asStateFlow()

    private val _customThemes = MutableStateFlow<List<CustomTheme>>(emptyList())
    val customThemes: StateFlow<List<CustomTheme>> = _customThemes.asStateFlow()

    suspend fun init() = withContext(Dispatchers.IO) {
        loadCustomThemes()
        loadActiveTheme()
    }

    suspend fun createCustomTheme(name: String, theme: Theme): Result<CustomTheme> = withContext(Dispatchers.IO) {
        if (!theme.isValid()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid theme colors"))
        }

        val now = TimeUtils.now()
        val id = "custom_$now"
        val customTheme = CustomTheme(
            id = id,
            name = name,
            theme = theme,
            createdAt = now,
            updatedAt = now,
            isActive = false
        )

        queries.insertCustomTheme(
            id = id,
            name = name,
            theme_json = json.encodeToString(theme),
            created_at = now,
            updated_at = now,
            is_active = 0
        )

        loadCustomThemes()
        Result.success(customTheme)
    }

    suspend fun updateCustomTheme(id: String, name: String, theme: Theme): Result<Unit> = withContext(Dispatchers.IO) {
        if (!theme.isValid()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid theme colors"))
        }

        val existing = queries.getCustomTheme(id).executeAsOneOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("Theme not found"))

        queries.insertCustomTheme(
            id = id,
            name = name,
            theme_json = json.encodeToString(theme),
            created_at = existing.created_at,
            updated_at = TimeUtils.now(),
            is_active = existing.is_active
        )

        loadCustomThemes()
        if (existing.is_active == 1L) {
            _activeTheme.value = theme
        }
        Result.success(Unit)
    }

    suspend fun deleteCustomTheme(id: String) = withContext(Dispatchers.IO) {
        queries.deleteCustomTheme(id)
        loadCustomThemes()
    }

    suspend fun duplicateCustomTheme(id: String): Result<CustomTheme> = withContext(Dispatchers.IO) {
        val existing = queries.getCustomTheme(id).executeAsOneOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("Theme not found"))

        val theme = json.decodeFromString<Theme>(existing.theme_json)
        createCustomTheme("${existing.name} (Copy)", theme)
    }

    suspend fun setActivePreset(preset: ThemePreset) = withContext(Dispatchers.IO) {
        queries.deactivateAllThemes()
        storage.saveString("active_preset", preset.name)
        storage.saveString("active_theme_id", "")
        _activeTheme.value = preset.theme
    }

    suspend fun setActiveCustomTheme(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val theme = queries.getCustomTheme(id).executeAsOneOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("Theme not found"))

        queries.deactivateAllThemes()
        queries.activateTheme(id)
        storage.saveString("active_theme_id", id)
        storage.saveString("active_preset", "")

        _activeTheme.value = json.decodeFromString(theme.theme_json)
        Result.success(Unit)
    }

    suspend fun exportTheme(id: String): Result<String> = withContext(Dispatchers.IO) {
        val theme = queries.getCustomTheme(id).executeAsOneOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("Theme not found"))

        Result.success(theme.theme_json)
    }

    suspend fun exportAllThemes(): String = withContext(Dispatchers.IO) {
        val themes = queries.getAllCustomThemes().executeAsList()
        json.encodeToString(themes.map { json.decodeFromString<Theme>(it.theme_json) })
    }

    suspend fun importTheme(name: String, themeJson: String): Result<CustomTheme> = withContext(Dispatchers.IO) {
        try {
            val theme = json.decodeFromString<Theme>(themeJson)
            if (!theme.isValid()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid theme format"))
            }
            createCustomTheme(name, theme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadCustomThemes() {
        val themes = queries.getAllCustomThemes().executeAsList().map {
            CustomTheme(
                id = it.id,
                name = it.name,
                theme = json.decodeFromString(it.theme_json),
                createdAt = it.created_at,
                updatedAt = it.updated_at,
                isActive = it.is_active == 1L
            )
        }
        _customThemes.value = themes
    }

    private suspend fun loadActiveTheme() {
        val activeThemeId = storage.getString("active_theme_id", "")
        if (activeThemeId.isNotEmpty()) {
            val theme = queries.getCustomTheme(activeThemeId).executeAsOneOrNull()
            if (theme != null) {
                _activeTheme.value = json.decodeFromString(theme.theme_json)
                return
            }
        }

        val activePreset = storage.getString("active_preset", ThemePreset.MIDNIGHT.name)
        try {
            _activeTheme.value = ThemePreset.valueOf(activePreset).theme
        } catch (e: Exception) {
            _activeTheme.value = ThemePreset.MIDNIGHT.theme
        }
    }
}
