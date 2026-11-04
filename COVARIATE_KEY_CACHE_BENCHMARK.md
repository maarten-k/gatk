# CovariateKeyCache Performance Benchmark

## Summary

Replaced `LRUCache<Integer, int[][][]>` (using LinkedHashMap with Integer keys) with FastUtil's `Int2ObjectLinkedOpenHashMap<int[][][]>` (using primitive int keys) to eliminate boxing overhead.

## Results

### Micro-Benchmarks (10M iterations)

| Operation | Old (LinkedHashMap) | New (FastUtil) | Speedup |
|-----------|---------------------|----------------|---------|
| Raw map get | 17 ns/op | 7 ns/op | **2.43x** |
| LRU cache get | 31 ns/op | 11 ns/op | **2.63x** |

### Realistic BQSR Scenarios (5M iterations)

| Scenario | Old | New | Speedup |
|----------|-----|-----|---------|
| Common read length (90% same, 10% varying) | 30 ns/op | 14 ns/op | **2.13x** |
| Many different read lengths (LRU stress) | 40 ns/op | 28 ns/op | **1.40x** |

### Full Cache Operations (1M iterations, 80% hits, 20% misses)

| Operation | Old | New | Speedup |
|-----------|-----|-----|---------|
| Get (cache hits/misses) | 55 ns/op | 45 ns/op | **1.22x** |
| Put (sequential, wraps) | 58 ns/op | 52 ns/op | **1.12x** |

## Key Improvements

1. **Eliminated boxing overhead**: Primitive `int` keys avoid Integer object creation
2. **Faster map operations**: FastUtil's primitive-specialized maps are highly optimized
3. **Maintained LRU semantics**: Same eviction policy with `getAndMoveToLast()` and `putAndMoveToLast()`

## Impact

For a typical BQSR run processing millions of reads, this optimization can save **seconds to minutes** of runtime depending on the dataset size, with the most significant gains when processing reads with common lengths (the typical case).

## Files Modified

- `src/main/java/org/broadinstitute/hellbender/utils/recalibration/covariates/CovariateKeyCache.java`

## Benchmark Files

- `src/test/java/org/broadinstitute/hellbender/utils/recalibration/covariates/CovariateKeyCacheBenchmark.java`
- `src/test/java/org/broadinstitute/hellbender/utils/recalibration/covariates/CovariateKeyCacheMicroBenchmark.java`
- `src/test/java/org/broadinstitute/hellbender/utils/recalibration/covariates/CovariateKeyCacheRealisticBenchmark.java`

Run benchmarks with:
```bash
cd /home/maartenk/git/gatkbqsr
FASTUTIL_JAR=$(find ~/.gradle/caches -name "fastutil-*.jar" | head -1)
javac -cp "$FASTUTIL_JAR" -d /tmp/benchmark src/main/java/org/broadinstitute/hellbender/utils/LRUCache.java src/test/java/.../CovariateKeyCache*.java
cd /tmp/benchmark && java -cp ".:$FASTUTIL_JAR" org.broadinstitute.hellbender.utils.recalibration.covariates.CovariateKeyCache*
```
