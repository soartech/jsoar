/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel;

/**
 * Base exception class used throughout JSoar
 * 
 * @author ray
 */
public class SoarException extends Exception
{
    private static final long serialVersionUID = 6819941600876892428L;

    /**
     * 
     */
    public SoarException()
    {
    }

    /**
     * @param message
     */
    public SoarException(String message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public SoarException(Throwable cause)
    {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SoarException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
