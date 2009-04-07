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
    public static void checkNotNull(Object arg, String name)
    {
        if(arg == null)
        {
            throw new IllegalArgumentException("'" + name + "' must not be null");
        }
    }
    
    public static void check(boolean condition, String description)
    {
        if(!condition)
        {
            throw new IllegalArgumentException(description);
        }
    }
}
