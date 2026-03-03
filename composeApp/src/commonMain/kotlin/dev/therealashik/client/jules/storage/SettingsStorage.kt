package dev.therealashik.client.jules.storage

import kotlinx.coroutines.flow.Flow

// TODO: Add encrypted storage support for sensitive data (API keys)
expect class SettingsStorage() {
    suspend fun saveString(key: String, value: String)
    suspend fun getString(key: String, default: String): String
    suspend fun saveBoolean(key: String, value: Boolean)
    suspend fun getBoolean(key: String, default: Boolean): Boolean
    suspend fun saveLong(key: String, value: Long)
    suspend fun getLong(key: String, default: Long): Long
    suspend fun delete(key: String)
    suspend fun clear()
    fun observeString(key: String, default: String): Flow<String>
}
