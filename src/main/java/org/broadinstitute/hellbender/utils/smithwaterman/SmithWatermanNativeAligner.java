package org.broadinstitute.hellbender.utils.smithwaterman;


import com.intel.gkl.smithwaterman.IntelSmithWaterman;
import org.broadinstitute.gatk.nativebindings.smithwaterman.SWOverhangStrategy;
import org.broadinstitute.gatk.nativebindings.smithwaterman.SWParameters;
import org.broadinstitute.gatk.nativebindings.smithwaterman.SWAlignerNativeBinding;
import org.broadinstitute.hellbender.exceptions.UserException;
/**
 * SmithWatermanIntelAligner class that converts instance of {@link SWAlignerNativeBinding} into a {@link SmithWatermanIntelAligner}
 * This is optimized for Intel Architectures and can fail if Machine does not support AVX and will throw {@link UserException}
 */
public final class SmithWatermanNativeAligner implements SmithWatermanAligner {

    private static final Logger logger = LogManager.getLogger(SmithWatermanNativeAligner.class);


    private final SWAlignerNativeBinding smithWaterman = new IntelSmithWaterman();

    /*
    * Generate SWAlignerWrapper instance
    */
    private final SWNativeAlignerWrapper alignerWrapper = new SWNativeAlignerWrapper(smithWaterman);

    public SmithWatermanNativeAligner() throws UserException.HardwareFeatureException {
        if (!smithWaterman.load(null)) {
            throw new UserException.HardwareFeatureException("The native Smith-Waterman library is not available on this platform.");
        }
    }

    /**
     * Aligns the alternate sequence to the reference sequence
     *
     * @param reference  ref sequence
     * @param alternate  alt sequence
     */
    @Override
    public SmithWatermanAlignment align(final byte[] reference, final byte[] alternate, final SWParameters parameters, final SWOverhangStrategy overhangStrategy) {
        return alignerWrapper.align(reference, alternate, parameters, overhangStrategy);
    }

    /**
     * Close the aligner
     */
    @Override
    public void close() {
        alignerWrapper.close();
    }
}
