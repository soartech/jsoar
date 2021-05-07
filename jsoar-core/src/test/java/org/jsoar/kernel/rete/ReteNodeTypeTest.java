/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2010
 */
package org.jsoar.kernel.rete;

import static org.junit.Assert.*;

import org.junit.Test;

public class ReteNodeTypeTest {
  @Test
  public void testMPNodesArePosNeg() {
    assertTrue(ReteNodeType.MP_BNODE.bnode_is_posneg());
    assertTrue(ReteNodeType.UNHASHED_MP_BNODE.bnode_is_posneg());
  }

  @Test
  public void testMPNodesArePositive() {
    assertTrue(ReteNodeType.MP_BNODE.bnode_is_positive());
    assertTrue(ReteNodeType.UNHASHED_MP_BNODE.bnode_is_positive());
  }
}
