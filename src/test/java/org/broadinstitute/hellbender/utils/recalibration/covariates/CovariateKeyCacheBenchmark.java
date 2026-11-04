package org.broadinstitute.hellbender.utils.recalibration.covariates;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.broadinstitute.hellbender.utils.LRUCache;

import java.util.Random;

/**
 * Benchmark comparing the old LRUCache<Integer, int[][][]> implementation
 * with the new Int2ObjectLinkedOpenHashMap<int[][][]> implementation.
 */
public final class CovariateKeyCacheBenchmark {

    private static final int LRU_CACHE_SIZE = 500;
    private static final int WARMUP_ITERATIONS = 100_000;
    private static final int BENCHMARK_ITERATIONS = 1_000_000;
    private static final int NUM_KEYS = 1000;

    // Old implementation using LRUCache with Integer keys
    private static class OldCovariateKeyCache {
        private final LRUCache<Integer, int[][][]> keysCache = new LRUCache<>(LRU_CACHE_SIZE);

        public int[][][] get(final int readLength) {
            return keysCache.get(readLength);
        }

        public void put(final int readLength, final int[][][] keys) {
            keysCache.put(readLength, keys);
        }

        public int size() {
            return keysCache.size();
        }
    }

    // New implementation using FastUtil
    private static class NewCovariateKeyCache {
        private final Int2ObjectLinkedOpenHashMap<int[][][]> keysCache = new Int2ObjectLinkedOpenHashMap<>();

        public int[][][] get(final int readLength) {
            return keysCache.getAndMoveToLast(readLength);
        }

        public void put(final int readLength, final int[][][] keys) {
            if (keysCache.containsKey(readLength)) {
                keysCache.putAndMoveToLast(readLength, keys);
            } else {
                if (keysCache.size() >= LRU_CACHE_SIZE) {
                    keysCache.removeFirst();
                }
                keysCache.putAndMoveToLast(readLength, keys);
            }
        }

        public int size() {
            return keysCache.size();
        }
    }

    // Helper to create a 3D int array
    private static int[][][] create3DArray(int size) {
        int[][][] arr = new int[size][size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    arr[i][j][k] = i + j + k;
                }
            }
        }
        return arr;
    }

    public static void main(String[] args) {
        System.out.println("CovariateKeyCache Benchmark");
        System.out.println("===========================");
        System.out.println();

        // Generate access pattern: mix of cache hits and misses
        final Random random = new Random(42);
        final int[] accessKeys = new int[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            // 80% cache hits (keys 0-499), 20% cache misses (keys 500-999)
            if (random.nextDouble() < 0.8) {
                accessKeys[i] = random.nextInt(LRU_CACHE_SIZE);
            } else {
                accessKeys[i] = LRU_CACHE_SIZE + random.nextInt(NUM_KEYS - LRU_CACHE_SIZE);
            }
        }

        // Create test values
        final int[][][] testValue = create3DArray(5);

        // Benchmark old implementation
        System.out.println("Warming up old implementation...");
        OldCovariateKeyCache oldCache = new OldCovariateKeyCache();
        warmupOld(oldCache, testValue);
        
        System.out.println("Benchmarking old implementation...");
        long oldGetTime = benchmarkOldGet(oldCache, testValue, accessKeys);
        long oldPutTime = benchmarkOldPut(oldCache, testValue);

        // Benchmark new implementation
        System.out.println("Warming up new implementation...");
        NewCovariateKeyCache newCache = new NewCovariateKeyCache();
        warmupNew(newCache, testValue);
        
        System.out.println("Benchmarking new implementation...");
        long newGetTime = benchmarkNewGet(newCache, testValue, accessKeys);
        long newPutTime = benchmarkNewPut(newCache, testValue);

        // Print results
        System.out.println();
        System.out.println("Results (milliseconds, lower is better):");
        System.out.println("-------------------------------------");
        System.out.printf("%-25s %10s %10s %10s%n", "Operation", "Old (ms)", "New (ms)", "Speedup");
        System.out.println("-------------------------------------");
        
        double getSpeedup = (double) oldGetTime / newGetTime;
        double putSpeedup = (double) oldPutTime / newPutTime;
        
        System.out.printf("%-25s %10d %10d %9.2fx%n", "Get (80% hits, 20% misses)", oldGetTime, newGetTime, getSpeedup);
        System.out.printf("%-25s %10d %10d %9.2fx%n", "Put (sequential, wraps)", oldPutTime, newPutTime, putSpeedup);
        System.out.println();
        
        System.out.println("Detailed breakdown:");
        System.out.println("-------------------");
        System.out.printf("Old get: %,d ns/op%n", (oldGetTime * 1_000_000L) / BENCHMARK_ITERATIONS);
        System.out.printf("New get: %,d ns/op%n", (newGetTime * 1_000_000L) / BENCHMARK_ITERATIONS);
        System.out.printf("Old put: %,d ns/op%n", (oldPutTime * 1_000_000L) / BENCHMARK_ITERATIONS);
        System.out.printf("New put: %,d ns/op%n", (newPutTime * 1_000_000L) / BENCHMARK_ITERATIONS);
        System.out.println();
        System.out.printf("Get improvement: %.1f%% faster%n", (getSpeedup - 1) * 100);
        System.out.printf("Put improvement: %.1f%% faster%n", (putSpeedup - 1) * 100);
    }

    private static void warmupOld(OldCovariateKeyCache cache, int[][][] testValue) {
        // Pre-populate cache
        for (int i = 0; i < LRU_CACHE_SIZE; i++) {
            cache.put(i, testValue);
        }
        
        // Warmup
        Random random = new Random(42);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int key = random.nextInt(NUM_KEYS);
            if (random.nextDouble() < 0.8) {
                cache.get(key % LRU_CACHE_SIZE);
            } else {
                cache.get(key);
            }
        }
    }

    private static long benchmarkOldGet(OldCovariateKeyCache cache, int[][][] testValue, int[] accessKeys) {
        // Pre-populate cache
        for (int i = 0; i < LRU_CACHE_SIZE; i++) {
            cache.put(i, testValue);
        }
        
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            cache.get(accessKeys[i]);
        }
        long end = System.nanoTime();
        return (end - start) / 1_000_000; // Convert to milliseconds
    }

    private static long benchmarkOldPut(OldCovariateKeyCache cache, int[][][] testValue) {
        cache = new OldCovariateKeyCache(); // Start fresh
        
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            cache.put(i % LRU_CACHE_SIZE, testValue);
        }
        long end = System.nanoTime();
        return (end - start) / 1_000_000; // Convert to milliseconds
    }

    private static void warmupNew(NewCovariateKeyCache cache, int[][][] testValue) {
        // Pre-populate cache
        for (int i = 0; i < LRU_CACHE_SIZE; i++) {
            cache.put(i, testValue);
        }
        
        // Warmup
        Random random = new Random(42);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int key = random.nextInt(NUM_KEYS);
            if (random.nextDouble() < 0.8) {
                cache.get(key % LRU_CACHE_SIZE);
            } else {
                cache.get(key);
            }
        }
    }

    private static long benchmarkNewGet(NewCovariateKeyCache cache, int[][][] testValue, int[] accessKeys) {
        // Pre-populate cache
        for (int i = 0; i < LRU_CACHE_SIZE; i++) {
            cache.put(i, testValue);
        }
        
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            cache.get(accessKeys[i]);
        }
        long end = System.nanoTime();
        return (end - start) / 1_000_000; // Convert to milliseconds
    }

    private static long benchmarkNewPut(NewCovariateKeyCache cache, int[][][] testValue) {
        cache = new NewCovariateKeyCache(); // Start fresh
        
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            cache.put(i % LRU_CACHE_SIZE, testValue);
        }
        long end = System.nanoTime();
        return (end - start) / 1_000_000; // Convert to milliseconds
    }
}
