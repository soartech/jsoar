/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.legilimens.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;


/**
 * @author ray
 */
public class RingBufferTest
{
    
    @Test
    public void testWrite()
    {
        final RingBuffer b = new RingBuffer(4);
        b.write(new byte[] { 1 }, 0, 1);
        assertEquals(1, b.getHead());
        assertTrue(Arrays.equals(new byte[] { 1, 0, 0, 0 }, b.getRawBuffer()));
        
        b.write(new byte[] { 2 }, 0, 1);
        assertEquals(2, b.getHead());
        assertTrue(Arrays.equals(new byte[] { 1, 2, 0, 0 }, b.getRawBuffer()));

        b.write(new byte[] { 3, 4 }, 0, 2);
        assertEquals(0, b.getHead());
        assertTrue(Arrays.equals(new byte[] { 1, 2, 3, 4 }, b.getRawBuffer()));
        
        b.write(new byte[] { 5, 6, 7 }, 0, 3);
        assertEquals(3, b.getHead());
        assertTrue(Arrays.equals(new byte[] { 5, 6, 7, 4 }, b.getRawBuffer()));

        b.write(new byte[] { 8, 9, 10, 11, 12, 13 }, 0, 6);
        assertEquals(1, b.getHead());
        assertTrue(Arrays.equals(new byte[] { 13, 10, 11, 12 }, b.getRawBuffer()));
    }

    @Test
    public void testGetBytes()
    {
        final RingBuffer b = new RingBuffer(4);
        assertTrue(Arrays.equals(new byte[] { }, b.getRecentBytes(0)));
        
        b.write(new byte[] { 1 }, 0, 1);
        assertTrue(Arrays.equals(new byte[] { 1 }, b.getRecentBytes(1)));
        
        b.write(new byte[] { 2 }, 0, 1);
        assertTrue(Arrays.equals(new byte[] { 2 }, b.getRecentBytes(1)));
        assertTrue(Arrays.equals(new byte[] { 1, 2 }, b.getRecentBytes(2)));

        b.write(new byte[] { 3 }, 0, 1);
        assertTrue(Arrays.equals(new byte[] { 3 }, b.getRecentBytes(1)));
        assertTrue(Arrays.equals(new byte[] { 2, 3 }, b.getRecentBytes(2)));
        assertTrue(Arrays.equals(new byte[] { 1, 2, 3 }, b.getRecentBytes(3)));

        b.write(new byte[] { 4 }, 0, 1);
        assertTrue(Arrays.equals(new byte[] { 4 }, b.getRecentBytes(1)));
        assertTrue(Arrays.equals(new byte[] { 3, 4 }, b.getRecentBytes(2)));
        assertTrue(Arrays.equals(new byte[] { 2, 3, 4 }, b.getRecentBytes(3)));
        assertTrue(Arrays.equals(new byte[] { 1, 2, 3, 4 }, b.getRecentBytes(4)));

        b.write(new byte[] { 5 }, 0, 1);
        assertTrue(Arrays.equals(new byte[] { 5 }, b.getRecentBytes(1)));
        assertTrue(Arrays.equals(new byte[] { 4, 5 }, b.getRecentBytes(2)));
        assertTrue(Arrays.equals(new byte[] { 3, 4, 5 }, b.getRecentBytes(3)));
        assertTrue(Arrays.equals(new byte[] { 2, 3, 4, 5 }, b.getRecentBytes(4)));
    }
}
