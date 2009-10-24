/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.legilimens.trace;

import java.util.Arrays;


/**
 * @author ray
 */
public class RingBuffer
{
    private final byte[] buffer;
    private int head; // the next index to write to
    
    public RingBuffer(int size)
    {
        buffer = new byte[size];
        head = 0;
    }
    
    public int size()
    {
        return buffer.length;
    }
    
    public void write(byte[] bytes, int start, int length)
    {
        int end = start + length;
        int available = buffer.length - head;
        while(start < end)
        {
            int toWrite = Math.min(end - start, available);
            
            System.arraycopy(bytes, start, buffer, head, toWrite);
            
            head = (head + toWrite) % buffer.length;
            start += toWrite;
            available = buffer.length - head;
        }
    }
    
    public byte[] getRecentBytes(int count)
    {
        if(count < 0)
        {
            throw new IllegalArgumentException("count must be positive");
        }
        if(count > buffer.length)
        {
            throw new IllegalArgumentException("count must be < " + buffer.length);
        }
        
        int startPoint = head - count;
        if(startPoint < 0)
        {
            startPoint += buffer.length;
        }
        final int endPoint = head;
        
        if(startPoint <= endPoint && count != buffer.length)
        {
            return Arrays.copyOfRange(buffer, startPoint, endPoint);
        }
        else
        {
            final byte[] result = new byte[count];
            final int firstLength = buffer.length - startPoint;
            System.arraycopy(buffer, startPoint, result, 0, firstLength);
            System.arraycopy(buffer, 0, result, firstLength, endPoint);
            return result;
        }
    }
    
    int getHead()
    {
        return head;
    }
    byte[] getRawBuffer()
    {
        return buffer;
    }
}
