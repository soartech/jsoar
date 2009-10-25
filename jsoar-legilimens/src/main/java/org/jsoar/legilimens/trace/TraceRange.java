/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 24, 2009
 */
package org.jsoar.legilimens.trace;

/**
 * @author ray
 */
public class TraceRange
{
    private final int start;
    private final char[] data;
    private final int length;
    
    public TraceRange(int start, char[] data)
    {
        this(start, data, data.length);
    }
    
    public TraceRange(int start, char[] data, int length)
    {
        this.start = start;
        this.data = data;
        this.length = length;
    }
    
    public int getStart()
    {
        return start;
    }
    
    public int getEnd()
    {
        return start + length;
    }
    
    public int getLength()
    {
        return length;
    }
    
    public char[] getData()
    {
        return data;
    }
}
