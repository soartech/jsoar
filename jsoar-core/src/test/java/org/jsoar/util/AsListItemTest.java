/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;


public class AsListItemTest
{
    @Test
    public void testInsertAtHead()
    {
        ListHead<String> head = ListHead.newInstance();
        assertTrue(head.isEmpty());
        
        ListItem<String> a = new ListItem<String>("a");
        a.insertAtHead(head);
        assertSame(head.first, a);
        assertNull(a.previous);
        assertNull(a.next);
        
        ListItem<String> b = new ListItem<String>("b");
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
        ListItem<String> a = head.first;
        ListItem<String> b = a.next;
        ListItem<String> c = b.next;
        assertNotNull(c);
        assertNull(c.next);
        assertEquals("a", a.item);
        assertEquals("b", b.item);
        assertEquals("c", c.item);
        
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
