package dev.therealashik.client.jules.cache

import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.client.jules.model.CacheConfig
import dev.therealashik.client.jules.model.CacheStats
import kotlinx.coroutines.*
import dev.therealashik.client.jules.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CacheManager(
    private val db: JulesDatabase,
    private val config: CacheConfig,
    private val scope: CoroutineScope
) {
    private val queries = db.julesDatabaseQueries
    private val _stats = MutableStateFlow(loadStats())
    val stats: StateFlow<CacheStats> = _stats.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                delay(60_000) // Prune every minute
                pruneExpired()
            }
        }
    }

    suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        if (!config.enabled) return@withContext null

        val now = TimeUtils.now()
        val entry = queries.getCacheEntry(key, now).executeAsOneOrNull()

        if (entry != null) {
            queries.incrementAccessCount(key)
            queries.incrementHitCount()
            updateStats()
            entry.value_
        } else {
            queries.incrementMissCount()
            updateStats()
            null
        }
    }

    suspend fun set(key: String, value: String, ttlMs: Long = config.getExpirationMs()) = withContext(Dispatchers.IO) {
        if (!config.enabled) return@withContext

        val now = TimeUtils.now()
        val expiresAt = if (ttlMs == Long.MAX_VALUE) Long.MAX_VALUE else now + ttlMs
        val sizeBytes = value.encodeToByteArray().size.toLong()

        // Check size limit
        val currentSize = queries.getCacheSize().executeAsOne()
        val sizeLimit = config.getSizeLimitBytes()

        if (currentSize + sizeBytes > sizeLimit && sizeLimit != Long.MAX_VALUE) {
            // Evict LRU entries
            val entriesToEvict = ((currentSize + sizeBytes - sizeLimit) / 1024).toInt() + 1
            queries.evictLRU(entriesToEvict.toLong())
        }

        queries.insertCacheEntry(key, value, now, expiresAt, sizeBytes, key)
        updateStats()
    }

    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        queries.deleteCacheEntry(key)
        updateStats()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        queries.clearCache()
        queries.updateCacheMetadata(0, 0, TimeUtils.now(), 0, 0)
        updateStats()
    }

    suspend fun clearByPrefix(prefix: String) = withContext(Dispatchers.IO) {
        // FIXME: SQLite doesn't have a direct way to delete by prefix efficiently
        // This is a simplified version - needs proper implementation with LIKE query
        queries.transaction {
            val allKeys = queries.getCacheCount().executeAsOne()
            // In production, you'd want a better approach
        }
        updateStats()
    }

    private suspend fun pruneExpired() = withContext(Dispatchers.IO) {
        val now = TimeUtils.now()
        queries.pruneExpiredCache(now)
        queries.updateCacheMetadata(
            queries.getCacheSize().executeAsOne(),
            queries.getCacheCount().executeAsOne(),
            now,
            queries.getCacheMetadata().executeAsOneOrNull()?.hit_count ?: 0,
            queries.getCacheMetadata().executeAsOneOrNull()?.miss_count ?: 0
        )
        updateStats()
    }

    private fun loadStats(): CacheStats {
        val metadata = queries.getCacheMetadata().executeAsOneOrNull()
        return CacheStats(
            totalSizeBytes = queries.getCacheSize().executeAsOne(),
            entryCount = queries.getCacheCount().executeAsOne().toInt(),
            hitCount = metadata?.hit_count ?: 0,
            missCount = metadata?.miss_count ?: 0,
            lastPruned = metadata?.last_pruned ?: 0,
            lastCleared = 0
        )
    }

    private fun updateStats() {
        _stats.value = loadStats()
    }
}
