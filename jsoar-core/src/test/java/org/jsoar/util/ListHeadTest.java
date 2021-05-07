/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ListHeadTest {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testFromCollection() {
    List<String> strings = Arrays.asList("a", "b", "c", "d", "e", "f");
    ListHead<String> head = ListHead.fromCollection(strings);
    assertNotNull(head);
    assertEquals(strings.size(), head.size());
    assertEquals(strings, head.toList());
  }

  @Test
  public void testGetFirstItem() {
    List<String> strings = Arrays.asList("a", "b", "c", "d", "e", "f");
    ListHead<String> head = ListHead.fromCollection(strings);
    assertEquals("a", head.getFirstItem());
    head.clear();
    assertTrue(head.isEmpty());
    assertNull(head.getFirstItem());
  }

  @Test
  public void testPop() {
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
