package org.broadinstitute.hellbender.tools.spark.utils;

import org.apache.spark.api.java.function.Function;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.Serial;

public class ReadFilterSparkifier implements Function<GATKRead, Boolean> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ReadFilter filter;

    public ReadFilterSparkifier( final ReadFilter filter ) {this.filter = filter;}

    @Override
    public Boolean call( final GATKRead read ) { return filter.test(read); }
}
