/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 19, 2008
 */
package org.jsoar.util;

/**
 * Basic implementation of a by-reference argument used to emulate reference
 * arguments in C++.
 * 
 * @author ray
 */
public class ByRef<T>
{
    /**
     * The value held by this object. Accessing this is logically equivalent to
     * dereferencing a by-ref pointer in C.
     */
    public T value;
    
    /**
     * Convenience function to create a new ByRef.
     * 
     * Initially, there was an addition type parameter, U extends T, to make it
     * a little easier to use, but it turns out that Eclipse compiles this usage
     * fine, but the actual JDK compiler hates it. Way it goes.
     * 
     * @param <T> The type of value held by this object
     * @param value the initial value
     * @return a new ByRef holder object
     */
    public static <T> ByRef<T> create(T value)
    {
        return new ByRef<T>(value);
    }
    
    /**
     * Construct a new ByRef object holding <code>null</code>
     */
    public ByRef()
    {
        this.value = null;
    }
    
    /**
     * Construct a new ByRef object holding the given value.
     * 
     * @param value initial value
     */
    public ByRef(T value)
    {
        this.value = value;
    }
    
}
