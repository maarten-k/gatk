package org.broadinstitute.hellbender.utils.R;

import org.broadinstitute.hellbender.utils.runtime.ScriptExecutorException;

import java.io.Serial;

public final class RScriptExecutorException extends ScriptExecutorException {
    @Serial
    private static final long serialVersionUID = 0L;

    public RScriptExecutorException(String msg) {
        super(msg);
    }
}
