package org.jsoar.kernel.exceptions;

import org.jsoar.kernel.SoarException;

public class TclInterpreterException extends SoarException
{
    
    private static final long serialVersionUID = -6893950269471061294L;
    
    public TclInterpreterException(String message)
    {
        super(message);
    }
    
    /**
     * @param cause
     */
    public TclInterpreterException(Throwable cause)
    {
        super(cause);
    }
    
    /**
     * @param message
     * @param cause
     */
    public TclInterpreterException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
