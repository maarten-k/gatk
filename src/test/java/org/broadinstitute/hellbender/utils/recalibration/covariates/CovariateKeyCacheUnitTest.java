package org.broadinstitute.hellbender.utils.recalibration.covariates;

import org.broadinstitute.hellbender.GATKBaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class CovariateKeyCacheUnitTest extends GATKBaseTest {

    private static final int CACHE_CAPACITY = 500;

    @Test
    public void testEvictsLeastRecentlyUsedEntry() {
        final CovariateKeyCache cache = new CovariateKeyCache();
        final int[][][][] values = populateCache(cache);

        Assert.assertSame(cache.get(0), values[0]);
        cache.put(CACHE_CAPACITY, new int[0][][]);

        Assert.assertEquals(cache.size(), CACHE_CAPACITY);
        Assert.assertNull(cache.get(1));
        Assert.assertSame(cache.get(0), values[0]);
    }

    @Test
    public void testReplacingEntryAtCapacityDoesNotEvictAnotherEntry() {
        final CovariateKeyCache cache = new CovariateKeyCache();
        final int[][][][] values = populateCache(cache);
        final int[][][] replacement = new int[0][][];

        cache.put(0, replacement);

        Assert.assertEquals(cache.size(), CACHE_CAPACITY);
        Assert.assertSame(cache.get(0), replacement);
        for (int i = 1; i < CACHE_CAPACITY; i++) {
            Assert.assertSame(cache.get(i), values[i]);
        }
    }

    private static int[][][][] populateCache(final CovariateKeyCache cache) {
        final int[][][][] values = new int[CACHE_CAPACITY][][][];
        for (int i = 0; i < CACHE_CAPACITY; i++) {
            values[i] = new int[0][][];
            cache.put(i, values[i]);
        }
        Assert.assertEquals(cache.size(), CACHE_CAPACITY);
        return values;
    }
}
