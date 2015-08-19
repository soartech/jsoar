/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.util;


import android.test.AndroidTestCase;

import java.util.Arrays;
import java.util.List;

public class ListHeadTest extends AndroidTestCase
{

    @Override
    public void setUp() throws Exception
    {
    }

    @Override
    public void tearDown() throws Exception
    {
    }
    
    public void testFromCollection()
    {
        List<String> strings = Arrays.asList("a", "b", "c", "d", "e", "f");
        ListHead<String> head = ListHead.fromCollection(strings);
        assertNotNull(head);
        assertEquals(strings.size(), head.size());
        assertEquals(strings, head.toList());
    }
    
    public void testGetFirstItem()
    {
        List<String> strings = Arrays.asList("a", "b", "c", "d", "e", "f");
        ListHead<String> head = ListHead.fromCollection(strings);
        assertEquals("a", head.getFirstItem());
        head.clear();
        assertTrue(head.isEmpty());
        assertNull(head.getFirstItem());
        
    }
    
    public void testPop()
    {
        List<String> strings = Arrays.asList("a", "b", "c", "d", "e", "f");
        ListHead<String> head = ListHead.fromCollection(strings);
        
        assertEquals("a", head.pop());
        assertEquals("b", head.pop());
        assertEquals("c", head.pop());
        assertEquals("d", head.pop());
        assertEquals("e", head.pop());
        assertEquals("f", head.pop());
        assertNull(head.pop());
        assertTrue(head.isEmpty());
    }
}
