package org.broadinstitute.hellbender.utils.smithwaterman;

import org.broadinstitute.hellbender.exceptions.UserException;
import org.testng.SkipException;
import java.util.*;


/**
 * Runs the shared aligner tests against the native Smith-Waterman aligner, and checks that it produces exactly the
 * alignments the Java aligner does on many random sequence pairs.
 */
public class SmithWatermanNativeAlignerUnitTest extends SmithWatermanAlignerAbstractUnitTest {

    /*
    *Test the Intel Aligner Native Interface
    */


    @Override
    protected SmithWatermanNativeAligner getAligner() {
        try {
            return new SmithWatermanNativeAligner();
        } catch (final UserException.HardwareFeatureException e) {
            throw new SkipException("The native Smith-Waterman aligner is not available on this machine");
        }
        catch (UserException.HardwareFeatureException e ) {
            loaded = false;
        }
        if(!loaded) {
            //Skip test if correct version of AVX is not supported
            throw new SkipException("AVX SmithWaterman is not supported on this system or the library is not available");
        }
        return aligner;
    }

}
