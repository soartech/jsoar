/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 21, 2008
 */
package org.jsoar.kernel.io.quick;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

/**
 * @author ray
 */
public class DefaultQMemoryTest
{

    /**
     * Test method for {@link org.jsoar.kernel.io.quick.DefaultQMemory#create()}.
     */
    @Test
    public void testCreate()
    {
        QMemory q = DefaultQMemory.create();
        assertEquals(0, q.getPaths().size());
    }

    /**
     * Test method for {@link org.jsoar.kernel.io.quick.DefaultQMemory#hasPath(java.lang.String)}.
     */
    @Test
    public void testHasPath()
    {
        QMemory q = DefaultQMemory.create();
        assertEquals(0, q.getPaths().size());
        
        q.setDouble("a.b.c.d", 3.14);
        assertTrue(q.hasPath("a.b.c.d"));
        q.setString("a.b.c.e", "hello");
        assertTrue(q.hasPath("a.b.c.d"));
        assertTrue(q.hasPath("a.b.c.e"));
    }

    /**
     * Test method for {@link org.jsoar.kernel.io.quick.DefaultQMemory#subMemory(java.lang.String)}.
     */
    @Test
    public void testSubMemory()
    {
        QMemory top = DefaultQMemory.create();
        
        top.setString("a.b.c.d", "hello");
        top.setString("a.b.c.e", "goodbye");
        top.setString("a.b.c.f", "another");
        top.setString("x.b.c.f", "another");
        
        // Move down and only look at a.
        QMemory a = top.subMemory("a");
        assertEquals("hello", a.getString("b.c.d"));
        assertEquals("goodbye", a.getString("b.c.e"));
        assertEquals("another", a.getString("b.c.f"));
        
        // Make sure paths are filtered and trimmed
        assertEquals(new HashSet<String>(Arrays.asList("b.c.d", "b.c.e", "b.c.f")), a.getPaths());
        
        // Move down more and only look at a.b.c
        QMemory c = a.subMemory("b.c."); // trailing dot is handled by SubQMemory constructor
        assertEquals("hello", c.getString("d"));
        assertEquals("goodbye", c.getString("e"));
        assertEquals("another", c.getString("f"));
        
        // Make sure paths are filtered and trimmed
        assertEquals(new HashSet<String>(Arrays.asList("d", "e", "f")), c.getPaths());
    }
    
    @Test
    public void testMultiAttribute()
    {
        QMemory q = DefaultQMemory.create();
        
        q.setString("a.b[0]", "hi");
        q.setString("a.b[1]", "bye");
        
        //assertEquals("hi", q.getString("a.b"));
        assertEquals("hi", q.getString("a.b[0]"));
        assertEquals("bye", q.getString("a.b[1]"));
    }

}
