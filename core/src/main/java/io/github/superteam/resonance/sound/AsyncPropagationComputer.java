package io.github.superteam.resonance.sound;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asynchronous Dijkstra pathfinding computation system.
 * Runs propagation calculations on a background thread to avoid blocking the render thread.
 */
public final class AsyncPropagationComputer {
    private final ExecutorService executorService;
    private final DijkstraPathfinder dijkstraPathfinder;
    private final PropagationCache propagationCache;
    private final Map<PropagationCache.CacheKey, CompletableFuture<DualBandResult>> pendingComputations;
    private final AtomicBoolean shutdownRequested;

    public AsyncPropagationComputer(DijkstraPathfinder dijkstraPathfinder, PropagationCache propagationCache) {
        this.dijkstraPathfinder = Objects.requireNonNull(dijkstraPathfinder, "Dijkstra pathfinder must not be null.");
        this.propagationCache = Objects.requireNonNull(propagationCache, "Propagation cache must not be null.");
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "PropagationComputer");
            thread.setDaemon(true);
            return thread;
        });
        this.pendingComputations = new ConcurrentHashMap<>();
        this.shutdownRequested = new AtomicBoolean(false);
    }

    /**
     * Request propagation computation. Returns cached result if available,
     * or starts async computation and returns null.
     *
     * @param acousticGraphEngine The acoustic graph
     * @param sourceNodeId Source node ID
     * @param baseIntensity Base sound intensity
     * @param lowBandAttenuationAlpha Low band attenuation
     * @param highBandAttenuationAlpha High band attenuation
     * @param revealThreshold Reveal threshold
     * @param nowSeconds Current time in seconds
     * @return Cached result if available, null if computation is pending
     */
    public DualBandResult computeOrGetCached(
        AcousticGraphEngine acousticGraphEngine,
        String sourceNodeId,
        float baseIntensity,
        float lowBandAttenuationAlpha,
        float highBandAttenuationAlpha,
        float revealThreshold,
        float nowSeconds
    ) {
        if (shutdownRequested.get()) {
            return null;
        }

        PropagationCache.CacheKey cacheKey = PropagationCache.CacheKey.withRoundedIntensity(
            sourceNodeId,
            baseIntensity,
            lowBandAttenuationAlpha,
            highBandAttenuationAlpha,
            revealThreshold
        );

        // Check cache first
        DualBandResult cachedResult = propagationCache.getOrNull(cacheKey, nowSeconds);
        if (cachedResult != null) {
            return cachedResult;
        }

        // Check if computation is already pending
        CompletableFuture<DualBandResult> existingFuture = pendingComputations.get(cacheKey);
        if (existingFuture != null && !existingFuture.isDone()) {
            return null; // Computation in progress, return null to use stale data
        }

        // Start new async computation
        CompletableFuture<DualBandResult> future = CompletableFuture.supplyAsync(() -> {
            return dijkstraPathfinder.onSoundEventDualBand(
                acousticGraphEngine,
                sourceNodeId,
                baseIntensity,
                lowBandAttenuationAlpha,
                highBandAttenuationAlpha,
                revealThreshold
            );
        }, executorService).whenComplete((result, throwable) -> {
            pendingComputations.remove(cacheKey);
        });

        pendingComputations.put(cacheKey, future);

        // Store result in cache when complete
        future.thenAccept(result -> {
            if (result != null && !shutdownRequested.get()) {
                propagationCache.put(cacheKey, result, nowSeconds);
            }
        });

        return null; // Computation started, return null
    }

    /**
     * Check if a computation is pending for the given cache key.
     */
    public boolean isComputationPending(PropagationCache.CacheKey cacheKey) {
        CompletableFuture<?> future = pendingComputations.get(cacheKey);
        return future != null && !future.isDone();
    }

    /**
     * Cancel all pending computations and shutdown the executor.
     */
    public void shutdown() {
        shutdownRequested.set(true);
        pendingComputations.values().forEach(future -> future.cancel(true));
        pendingComputations.clear();
        executorService.shutdown();
    }

    /**
     * Get the number of pending computations.
     */
    public int getPendingComputationCount() {
        return pendingComputations.size();
    }
}
