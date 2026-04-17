package io.github.superteam.resonance.sound;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded LRU cache for dual-band propagation results.
 */
public final class PropagationCache {
    private static final int DEFAULT_MAX_CACHE_SIZE = 32;
    private static final float DEFAULT_CACHE_TTL_SECONDS = 0.8f;

    private final int maxCacheSize;
    private final float cacheTimeToLiveSeconds;
    private final LinkedHashMap<CacheKey, CacheEntry> cacheEntriesByKey;

    public PropagationCache() {
        this(DEFAULT_MAX_CACHE_SIZE, DEFAULT_CACHE_TTL_SECONDS);
    }

    public PropagationCache(int maxCacheSize, float cacheTimeToLiveSeconds) {
        if (maxCacheSize <= 0) {
            throw new IllegalArgumentException("Max cache size must be positive.");
        }
        if (cacheTimeToLiveSeconds <= 0f) {
            throw new IllegalArgumentException("Cache TTL seconds must be positive.");
        }

        this.maxCacheSize = maxCacheSize;
        this.cacheTimeToLiveSeconds = cacheTimeToLiveSeconds;
        this.cacheEntriesByKey = new LinkedHashMap<>(maxCacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheEntry> eldestEntry) {
                return size() > PropagationCache.this.maxCacheSize;
            }
        };
    }

    public DualBandResult getOrNull(CacheKey cacheKey, float nowSeconds) {
        Objects.requireNonNull(cacheKey, "Cache key must not be null.");
        CacheEntry cacheEntry = cacheEntriesByKey.get(cacheKey);
        if (cacheEntry == null) {
            return null;
        }
        if ((nowSeconds - cacheEntry.cachedAtSeconds()) > cacheTimeToLiveSeconds) {
            cacheEntriesByKey.remove(cacheKey);
            return null;
        }
        return cacheEntry.result();
    }

    public void put(CacheKey cacheKey, DualBandResult result, float nowSeconds) {
        Objects.requireNonNull(cacheKey, "Cache key must not be null.");
        Objects.requireNonNull(result, "Dual-band result must not be null.");
        cacheEntriesByKey.put(cacheKey, new CacheEntry(result, nowSeconds));
    }

    public void invalidateNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }
        cacheEntriesByKey.entrySet().removeIf(entry -> entry.getKey().sourceNodeId().equals(nodeId));
    }

    public void invalidateAll() {
        cacheEntriesByKey.clear();
    }

    public record CacheKey(
        String sourceNodeId,
        float baseIntensity,
        float lowBandAttenuationAlpha,
        float highBandAttenuationAlpha,
        float revealThreshold
    ) {
        public CacheKey {
            if (sourceNodeId == null || sourceNodeId.isBlank()) {
                throw new IllegalArgumentException("Source node id must not be blank.");
            }
            if (baseIntensity < 0f) {
                throw new IllegalArgumentException("Base intensity must be non-negative.");
            }
            if (lowBandAttenuationAlpha < 0f || highBandAttenuationAlpha < 0f) {
                throw new IllegalArgumentException("Attenuation alpha values must be non-negative.");
            }
            if (revealThreshold < 0f) {
                throw new IllegalArgumentException("Reveal threshold must be non-negative.");
            }
        }
    }

    private record CacheEntry(DualBandResult result, float cachedAtSeconds) {
    }
}
