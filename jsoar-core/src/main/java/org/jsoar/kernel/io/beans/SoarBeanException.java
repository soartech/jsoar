/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2009
 */
package org.jsoar.kernel.io.beans;

import org.jsoar.kernel.symbols.Identifier;

/**
 * Exception thrown when an error related to SoarBean reading or writing
 * occurs.
 * 
 * @author ray
 */
public class SoarBeanException extends Exception
{
    private static final long serialVersionUID = -8881847203939153443L;
    private final Class<?> klass;
    private final Identifier id;
    
    public Class<?> getSoarBeanClass()
    {
        return klass;
    }
    
    public Identifier getWmeIdentifier()
    {
        return id;
    }
    
    /**
     * 
     * @param message
     * @param klass - The SoarBean class
     * @param id - The root identifier of the SoarBean
     */
    public SoarBeanException(String message, Class<?> klass, Identifier id)
    {
        super(message);
        this.klass = klass;
        this.id = id;
    }
    
    /**
     * 
     * @param message
     * @param cause
     * @param klass - The SoarBean class
     * @param id - The root identifier of the SoarBean
     */
    public SoarBeanException(String message, Throwable cause, Class<?> klass, Identifier id)
    {
        super(message, cause);
        this.klass = klass;
        this.id = id;
    }

}
