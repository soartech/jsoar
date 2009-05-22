/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2009
 */
package org.jsoar.kernel.io.beans;

/**
 * @author ray
 */
public class SoarBeanException extends Exception
{
    private static final long serialVersionUID = -8881847203939153443L;

    /**
     * 
     */
    public SoarBeanException()
    {
    }

    /**
     * @param message
     */
    public SoarBeanException(String message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public SoarBeanException(Throwable cause)
    {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SoarBeanException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
