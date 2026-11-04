package org.broadinstitute.hellbender.utils.recalibration.covariates;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.broadinstitute.hellbender.utils.LRUCache;

import java.util.Random;

/**
 * Realistic benchmark simulating BQSR workload.
 * In BQSR, reads have various lengths, and for each read length we cache the covariate keys.
 * Typical read lengths range from ~35 to ~300 bases.
 */
public final class CovariateKeyCacheRealisticBenchmark {

    private static final int LRU_CACHE_SIZE = 500;
    private static final int WARMUP_ITERATIONS = 50_000;
    private static final int BENCHMARK_ITERATIONS = 5_000_000;

    // Realistic read lengths (Illumina sequencing)
    private static final int[] READ_LENGTHS = {
        35, 36, 37, 50, 51, 75, 76, 100, 101, 125, 126, 150, 151, 200, 250, 300
    };

    // Old implementation
    private static class OldCovariateKeyCache {
        private final LRUCache<Integer, int[][][]> keysCache = new LRUCache<>(LRU_CACHE_SIZE);

        public int[][][] get(final int readLength) {
            return keysCache.get(readLength);
        }

        public void put(final int readLength, final int[][][] keys) {
            keysCache.put(readLength, keys);
        }
    }

    // New implementation
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
    }

    private static int[][][] createCovariateKeys(int readLength) {
        // Simulate creating covariate keys for a read
        // In reality this is expensive, so we just create a small array
        return new int[5][readLength][10];
    }

    public static void main(String[] args) {
        System.out.println("Realistic BQSR CovariateKeyCache Benchmark");
        System.out.println("==========================================");
        System.out.println();

        final Random random = new Random(42);
        
        // Scenario 1: Sequential processing of reads with varying lengths
        // Most reads have the same length (common case)
        System.out.println("Scenario 1: Processing reads (90% same length, 10% varying)");
        System.out.println("-----------------------------------------------------------");
        
        // Generate access pattern: 90% reads of length 150, 10% random from READ_LENGTHS
        int[] accessPattern1 = new int[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            if (random.nextDouble() < 0.9) {
                accessPattern1[i] = 150; // Most common read length
            } else {
                accessPattern1[i] = READ_LENGTHS[random.nextInt(READ_LENGTHS.length)];
            }
        }
        
        OldCovariateKeyCache oldCache1 = new OldCovariateKeyCache();
        NewCovariateKeyCache newCache1 = new NewCovariateKeyCache();
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int len1 = accessPattern1[i % accessPattern1.length];
            int[][][] keys1 = oldCache1.get(len1);
            if (keys1 == null) {
                oldCache1.put(len1, createCovariateKeys(len1));
            }
            
            int len2 = accessPattern1[i % accessPattern1.length];
            int[][][] keys2 = newCache1.get(len2);
            if (keys2 == null) {
                newCache1.put(len2, createCovariateKeys(len2));
            }
        }
        
        // Benchmark old
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            int len = accessPattern1[i];
            int[][][] keys = oldCache1.get(len);
            if (keys == null) {
                oldCache1.put(len, createCovariateKeys(len));
            }
        }
        long oldTime1 = System.nanoTime() - start;
        
        // Benchmark new
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            int len = accessPattern1[i];
            int[][][] keys = newCache1.get(len);
            if (keys == null) {
                newCache1.put(len, createCovariateKeys(len));
            }
        }
        long newTime1 = System.nanoTime() - start;
        
        System.out.printf("Old cache: %,d ms total, %d ns/op%n", oldTime1 / 1_000_000, oldTime1 / BENCHMARK_ITERATIONS);
        System.out.printf("New cache: %,d ms total, %d ns/op%n", newTime1 / 1_000_000, newTime1 / BENCHMARK_ITERATIONS);
        System.out.printf("Speedup: %.2fx (%.1f%% faster)%n", 
            (double) oldTime1 / newTime1,
            ((double) oldTime1 / newTime1 - 1) * 100);
        System.out.println();

        // Scenario 2: Many different read lengths (stress test for LRU)
        System.out.println("Scenario 2: Many different read lengths (LRU stress test)");
        System.out.println("-----------------------------------------------------------");
        
        int[] accessPattern2 = new int[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            accessPattern2[i] = READ_LENGTHS[random.nextInt(READ_LENGTHS.length)];
        }
        
        OldCovariateKeyCache oldCache2 = new OldCovariateKeyCache();
        NewCovariateKeyCache newCache2 = new NewCovariateKeyCache();
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int len = accessPattern2[i % accessPattern2.length];
            oldCache2.get(len);
            newCache2.get(len);
        }
        
        // Benchmark old
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            int len = accessPattern2[i];
            int[][][] keys = oldCache2.get(len);
            if (keys == null) {
                oldCache2.put(len, createCovariateKeys(len));
            }
        }
        long oldTime2 = System.nanoTime() - start;
        
        // Benchmark new
        start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            int len = accessPattern2[i];
            int[][][] keys = newCache2.get(len);
            if (keys == null) {
                newCache2.put(len, createCovariateKeys(len));
            }
        }
        long newTime2 = System.nanoTime() - start;
        
        System.out.printf("Old cache: %,d ms total, %d ns/op%n", oldTime2 / 1_000_000, oldTime2 / BENCHMARK_ITERATIONS);
        System.out.printf("New cache: %,d ms total, %d ns/op%n", newTime2 / 1_000_000, newTime2 / BENCHMARK_ITERATIONS);
        System.out.printf("Speedup: %.2fx (%.1f%% faster)%n", 
            (double) oldTime2 / newTime2,
            ((double) oldTime2 / newTime2 - 1) * 100);
        System.out.println();

        // Summary
        System.out.println("Summary");
        System.out.println("-------");
        System.out.printf("Scenario 1 (common length): %.2fx faster%n", (double) oldTime1 / newTime1);
        System.out.printf("Scenario 2 (many lengths):  %.2fx faster%n", (double) oldTime2 / newTime2);
    }
}
