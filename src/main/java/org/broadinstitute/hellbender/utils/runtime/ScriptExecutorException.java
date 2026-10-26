package org.broadinstitute.hellbender.utils.runtime;

import org.broadinstitute.hellbender.exceptions.GATKException;

import java.io.Serial;

/**
 * Base type for exceptions thrown by the ScriptExecutor.
 */
public class ScriptExecutorException extends GATKException {
    @Serial
    private static final long serialVersionUID = 0L;

    public ScriptExecutorException(final String message) {
        super(message);
    }
}
