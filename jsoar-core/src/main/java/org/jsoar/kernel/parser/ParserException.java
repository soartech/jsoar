/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 13, 2008
 */
package org.jsoar.kernel.parser;

/**
 * @author ray
 */
public class ParserException extends Exception
{
    private static final long serialVersionUID = -2467524213111939219L;
    
    public ParserException(String message)
    {
        super(message);
    }
    
    /**
     * 
     */
    public ParserException()
    {
        super();
    }
    
    /**
     * @param message
     * @param cause
     */
    public ParserException(String message, Throwable cause)
    {
        super(message, cause);
    }
    
    /**
     * @param cause
     */
    public ParserException(Throwable cause)
    {
        super(cause);
    }
    
}
