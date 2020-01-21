/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

/**
 * @author ray
 */
public class RhsFunctionException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public RhsFunctionException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public RhsFunctionException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
