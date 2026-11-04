package org.broadinstitute.hellbender.utils.recalibration.covariates;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.broadinstitute.hellbender.utils.LRUCache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Micro-benchmark to measure the specific overhead of boxing vs primitive keys.
 * This isolates the performance difference between Integer keys and int keys.
 */
public final class CovariateKeyCacheMicroBenchmark {

    private static final int WARMUP_ITERATIONS = 100_000;
    private static final int BENCHMARK_ITERATIONS = 10_000_000;

    public static void main(String[] args) {
        System.out.println("CovariateKeyCache Micro-Benchmark (Boxing Overhead)");
        System.out.println("====================================================");
        System.out.println();

        // Test 1: Raw LinkedHashMap with Integer keys vs FastUtil with int keys
        System.out.println("Test 1: Raw Map Get Operations");
        System.out.println("--------------------------------");
        
        // LinkedHashMap with Integer keys
        Map<Integer, int[][][]> linkedHashMap = new LinkedHashMap<>();
        for (int i = 0; i < 500; i++) {
            linkedHashMap.put(i, new int[5][5][5]);
        }
        
        // Int2ObjectLinkedOpenHashMap with int keys
        Int2ObjectLinkedOpenHashMap<int[][][]> fastUtilMap = new Int2ObjectLinkedOpenHashMap<>();
        for (int i = 0; i < 500; i++) {
            fastUtilMap.put(i, new int[5][5][5]);
        }
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            linkedHashMap.get(i % 500);
            fastUtilMap.get(i % 500);
        }
        
        // Benchmark LinkedHashMap (with boxing)
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            linkedHashMap.get(i % 500);
        }
        long linkedHashMapTime = System.nanoTime() - start;
        
        // Benchmark FastUtil (no boxing)
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            fastUtilMap.get(i % 500);
        }
        long fastUtilTime = System.nanoTime() - start;
        
        System.out.printf("LinkedHashMap (Integer keys): %d ns/op%n", linkedHashMapTime / BENCHMARK_ITERATIONS);
        System.out.printf("FastUtil (int keys):          %d ns/op%n", fastUtilTime / BENCHMARK_ITERATIONS);
        System.out.printf("Speedup: %.2fx (%.1f%% faster)%n", 
            (double) linkedHashMapTime / fastUtilTime,
            ((double) linkedHashMapTime / fastUtilTime - 1) * 100);
        System.out.println();

        // Test 2: LRUCache with Integer keys vs FastUtil LRU simulation
        System.out.println("Test 2: LRU Cache Get Operations");
        System.out.println("---------------------------------");
        
        LRUCache<Integer, int[][][]> lruCache = new LRUCache<>(500);
        for (int i = 0; i < 500; i++) {
            lruCache.put(i, new int[5][5][5]);
        }
        
        Int2ObjectLinkedOpenHashMap<int[][][]> fastUtilLRU = new Int2ObjectLinkedOpenHashMap<>();
        for (int i = 0; i < 500; i++) {
            fastUtilLRU.putAndMoveToLast(i, new int[5][5][5]);
        }
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            lruCache.get(i % 500);
            fastUtilLRU.getAndMoveToLast(i % 500);
        }
        
        // Benchmark LRUCache
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            lruCache.get(i % 500);
        }
        long lruCacheTime = System.nanoTime() - start;
        
        // Benchmark FastUtil with LRU
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            fastUtilLRU.getAndMoveToLast(i % 500);
        }
        long fastUtilLRUTime = System.nanoTime() - start;
        
        System.out.printf("LRUCache (Integer, LinkedHashMap): %d ns/op%n", lruCacheTime / BENCHMARK_ITERATIONS);
        System.out.printf("FastUtil (int, LinkedOpenHashMap): %d ns/op%n", fastUtilLRUTime / BENCHMARK_ITERATIONS);
        System.out.printf("Speedup: %.2fx (%.1f%% faster)%n", 
            (double) lruCacheTime / fastUtilLRUTime,
            ((double) lruCacheTime / fastUtilLRUTime - 1) * 100);
        System.out.println();

        // Test 3: Measure boxing overhead directly
        System.out.println("Test 3: Direct Boxing Overhead");
        System.out.println("-------------------------------");
        
        // Just boxing int to Integer
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Integer boxed = i % 500;
        }
        long boxingTime = System.nanoTime() - start;
        
        System.out.printf("Boxing int -> Integer: %d ns/op%n", boxingTime / BENCHMARK_ITERATIONS);
    }
}
