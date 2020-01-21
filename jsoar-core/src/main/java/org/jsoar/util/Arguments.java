/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 19, 2008
 */
package org.jsoar.util;

/**
 * @author ray
 */
public final class Arguments
{
    /**
     * Throw an illegal argument exception if the given argument is {@code null}
     * 
     * @param arg the argument to check
     * @param name the name of the argument to be used in the exception message.
     */
    public static void checkNotNull(Object arg, String name)
    {
        if(arg == null)
        {
            throw new IllegalArgumentException("'" + name + "' must not be null");
        }
    }
    
    /**
     * If the given condition is not true, throw an {@link IllegalArgumentException}
     * with the given description.
     * 
     * @param condition the desired condition
     * @param description exception message
     */
    public static void check(boolean condition, String description)
    {
        if(!condition)
        {
            throw new IllegalArgumentException(description);
        }
    }
}
