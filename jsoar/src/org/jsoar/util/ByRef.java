/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 19, 2008
 */
package org.jsoar.util;

/**
 * @author ray
 */
public class ByRef<T>
{
    public T value;
    
    /**
     * Convenience function to create a new ByRef. 
     * 
     * Initially, there was an addition type parameter, U extends T, to make it
     * a little easier to use, but it turns out that Eclipse compiles this usage
     * fine, but the actual JDK compiler hates it. Way it goes.
     *  
     * @param <T>
     * @param value
     * @return
     */
    public static <T> ByRef<T> create(T value)
    {
        return new ByRef<T>(value);
    }

    public ByRef()
    {
        this.value = null;
    }
    
    /**
     * @param value
     */
    public ByRef(T value)
    {
        this.value = value;
    }
    
    
}
