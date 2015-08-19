/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2010
 */
package org.jsoar.kernel.rete;

import android.test.AndroidTestCase;


public class ReteNodeTypeTest extends AndroidTestCase
{
    public void testMPNodesArePosNeg()
    {
        assertTrue(ReteNodeType.MP_BNODE.bnode_is_posneg());
        assertTrue(ReteNodeType.UNHASHED_MP_BNODE.bnode_is_posneg());
    }
    
    public void testMPNodesArePositive()
    {
        assertTrue(ReteNodeType.MP_BNODE.bnode_is_positive());
        assertTrue(ReteNodeType.UNHASHED_MP_BNODE.bnode_is_positive());
    }
}
