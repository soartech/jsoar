/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.legilimens.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;


/**
 * @author ray
 */
public class RingBufferTest
{
    
    @Test
    public void testWrite()
    {
        final RingBuffer b = new RingBuffer(4);
        b.write(new char[] { 1 }, 0, 1);
        assertEquals(1, b.getHead());
        assertTrue(Arrays.equals(new char[] { 1, 0, 0, 0 }, b.getRawBuffer()));
        
        b.write(new char[] { 2 }, 0, 1);
        assertEquals(2, b.getHead());
        assertTrue(Arrays.equals(new char[] { 1, 2, 0, 0 }, b.getRawBuffer()));

        b.write(new char[] { 3, 4 }, 0, 2);
        assertEquals(0, b.getHead());
        assertTrue(Arrays.equals(new char[] { 1, 2, 3, 4 }, b.getRawBuffer()));
        
        b.write(new char[] { 5, 6, 7 }, 0, 3);
        assertEquals(3, b.getHead());
        assertTrue(Arrays.equals(new char[] { 5, 6, 7, 4 }, b.getRawBuffer()));

        b.write(new char[] { 8, 9, 10, 11, 12, 13 }, 0, 6);
        assertEquals(1, b.getHead());
        assertTrue(Arrays.equals(new char[] { 13, 10, 11, 12 }, b.getRawBuffer()));
    }

    @Test
    public void testGetBytes()
    {
        final RingBuffer b = new RingBuffer(4);
        assertTrue(Arrays.equals(new char[] { }, b.getTail(0)));
        
        b.write(new char[] { 1 }, 0, 1);
        assertTrue(Arrays.equals(new char[] { 1 }, b.getTail(1)));
        
        b.write(new char[] { 2 }, 0, 1);
        assertTrue(Arrays.equals(new char[] { 2 }, b.getTail(1)));
        assertTrue(Arrays.equals(new char[] { 1, 2 }, b.getTail(2)));

        b.write(new char[] { 3 }, 0, 1);
        assertTrue(Arrays.equals(new char[] { 3 }, b.getTail(1)));
        assertTrue(Arrays.equals(new char[] { 2, 3 }, b.getTail(2)));
        assertTrue(Arrays.equals(new char[] { 1, 2, 3 }, b.getTail(3)));

        b.write(new char[] { 4 }, 0, 1);
        assertTrue(Arrays.equals(new char[] { 4 }, b.getTail(1)));
        assertTrue(Arrays.equals(new char[] { 3, 4 }, b.getTail(2)));
        assertTrue(Arrays.equals(new char[] { 2, 3, 4 }, b.getTail(3)));
        assertTrue(Arrays.equals(new char[] { 1, 2, 3, 4 }, b.getTail(4)));

        b.write(new char[] { 5 }, 0, 1);
        assertTrue(Arrays.equals(new char[] { 5 }, b.getTail(1)));
        assertTrue(Arrays.equals(new char[] { 4, 5 }, b.getTail(2)));
        assertTrue(Arrays.equals(new char[] { 3, 4, 5 }, b.getTail(3)));
        assertTrue(Arrays.equals(new char[] { 2, 3, 4, 5 }, b.getTail(4)));
    }
    
    @Test
    public void getLimitedTail()
    {
        final RingBuffer b = new RingBuffer(4);
        b.write(new char[] { 1, 2, 3, 4, 5 }, 0, 5);
        assertTrue(Arrays.equals(new char[] { 2, 3, 4, 5 }, b.getTail(4)));
        
        assertTrue(Arrays.equals(new char[] { 2  }, b.getTail(4, 1)));
        assertTrue(Arrays.equals(new char[] { 2, 3 }, b.getTail(4, 2)));
        assertTrue(Arrays.equals(new char[] { 2, 3, 4}, b.getTail(4, 3)));
        assertTrue(Arrays.equals(new char[] { 2, 3, 4, 5 }, b.getTail(4, 4)));
        
        assertTrue(Arrays.equals(new char[] { 3 }, b.getTail(3, 1)));
        assertTrue(Arrays.equals(new char[] { 3, 4}, b.getTail(3, 2)));
        assertTrue(Arrays.equals(new char[] { 3, 4, 5 }, b.getTail(3, 3)));
    }
}
