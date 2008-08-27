/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.util;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;


public class AsListItemTest
{
    @Test
    public void testInsertAtHead()
    {
        ListHead<String> head = new ListHead<String>();
        assertTrue(head.isEmpty());
        
        AsListItem<String> a = new AsListItem<String>("a");
        a.insertAtHead(head);
        assertSame(head.first, a);
        assertNull(a.previous);
        assertNull(a.next);
        
        AsListItem<String> b = new AsListItem<String>("b");
        b.insertAtHead(head);
        assertSame(head.first, b);
        assertNull(b.previous);
        assertSame(a, b.next);
        assertSame(b, a.previous);
        assertNull(a.next);
    }
    
    @Test
    public void testRemove()
    {
        ListHead<String> head = ListHead.fromCollection(Arrays.asList("a", "b", "c"));
        AsListItem<String> a = head.first;
        AsListItem<String> b = a.next;
        AsListItem<String> c = b.next;
        assertNotNull(c);
        assertNull(c.next);
        assertEquals("a", a.get());
        assertEquals("b", b.get());
        assertEquals("c", c.get());
        
        b.remove(head);
        assertSame(a, head.first);
        assertNull(a.previous);
        assertSame(c, a.next);
        assertSame(a, c.previous);
        assertNull(c.next);
        
        a.remove(head);
        assertSame(c, head.first);
        assertNull(c.previous);
        assertNull(c.next);
        
        c.remove(head);
        assertNull(head.first);
        assertTrue(head.isEmpty());
        
    }
}
