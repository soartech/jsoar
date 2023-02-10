/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListHeadTest
{
    
    @BeforeEach
    public void setUp() throws Exception
    {
    }
    
    @AfterEach
    public void tearDown() throws Exception
    {
    }
    
    @Test
    public void testFromCollection()
    {
        List<String> strings = Arrays.asList("a", "b", "c", "d", "e", "f");
        ListHead<String> head = ListHead.fromCollection(strings);
        assertNotNull(head);
        assertEquals(strings.size(), head.size());
        assertEquals(strings, head.toList());
    }
    
    @Test
    public void testGetFirstItem()
    {
        List<String> strings = Arrays.asList("a", "b", "c", "d", "e", "f");
        ListHead<String> head = ListHead.fromCollection(strings);
        assertEquals("a", head.getFirstItem());
        head.clear();
        assertTrue(head.isEmpty());
        assertNull(head.getFirstItem());
        
    }
    
    @Test
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
