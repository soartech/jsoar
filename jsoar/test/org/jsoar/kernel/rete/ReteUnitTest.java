/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2008
 */
package org.jsoar.kernel.rete;


import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.util.AsListItem;
import org.junit.Test;

/**
 * @author ray
 */
public class ReteUnitTest extends JSoarTest
{
    @Test
    public void testInitDummyTopNode() throws Exception
    {
        Rete rete = new Rete(varGen);
        assertNotNull(rete.dummy_top_node);
        assertEquals(ReteNode.DUMMY_TOP_BNODE, rete.dummy_top_node.node_type);
        RightToken dummyTopToken = (RightToken) rete.dummy_top_node.a_np.tokens.first.get();
        assertNotNull(dummyTopToken);
        assertNull(dummyTopToken.parent);
        assertSame(rete.dummy_top_node, dummyTopToken.node);
    }

    @Test
    public void testAddProductionToRete() throws Exception
    {
        Parser parser = createParser(
           "testAddProductionToRete \n" +
           "(<root> ^integer 1 \n" +
           "        ^float 3.14 \n" +
           "        ^string |S| \n" +
           "        ^id <id>) \n" +
           "--> \n" +
           "(write <root>)");
        
        Production p = parser.parse_production();
        assertNotNull(p);
        
        Rete rete = new Rete(varGen);
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
    }
}
