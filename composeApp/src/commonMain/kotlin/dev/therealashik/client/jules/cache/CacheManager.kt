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
        queries.deleteCacheEntriesByPrefix("$prefix%")
        updateStats()
    }

    suspend fun getFrequentlyAccessedKeys(prefix: String, limit: Long): List<String> = withContext(Dispatchers.IO) {
        queries.getFrequentlyAccessedKeys("$prefix%", limit).executeAsList()
    }

    private suspend fun pruneExpired() = withContext(Dispatchers.IO) {
        val now = TimeUtils.now()
        queries.transaction {
            queries.pruneExpiredCache(now)
            queries.updateCacheMetadataPruned(now)
        }
        updateStats()
    }

    private fun loadStats(): CacheStats {
        val stats = queries.getCombinedCacheStats().executeAsOne()
        return CacheStats(
            totalSizeBytes = stats.total_size,
            entryCount = stats.entry_count.toInt(),
            hitCount = stats.hit_count,
            missCount = stats.miss_count,
            lastPruned = stats.last_pruned,
            lastCleared = 0
        )
    }

    private fun updateStats() {
        _stats.value = loadStats()
    }
}
