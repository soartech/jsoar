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
    
    public static <T, U extends T> ByRef<T> create(U value)
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
