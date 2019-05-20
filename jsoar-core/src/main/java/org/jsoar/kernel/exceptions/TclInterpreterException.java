package org.jsoar.kernel.exceptions;

import org.jsoar.kernel.SoarException;

public class TclInterpreterException extends SoarException {

    public TclInterpreterException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public TclInterpreterException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public TclInterpreterException(String message, Throwable cause) {
        super(message, cause);
    }
}
