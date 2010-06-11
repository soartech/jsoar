/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.timing.ExecutionTimer;
import org.junit.Test;

/**
 * @author ray
 */
public class ChunkingTests extends FunctionalTestHarness
{    
    @Test
    public void testJustifications() throws Exception
    {
        runTest("testJustifications", 2);
        Production j = agent.getProductions().getProduction("justification-1");
        assertNull(j);
    }
    
    @Test
    public void testChunks() throws Exception
    {
        runTest("testChunks", 2);
        
        // Verify that the chunk was created correctly
        JSoarTest.verifyProduction(agent, 
                "chunk-1*d2*opnochange*1", 
                ProductionType.CHUNK, 
                "sp {chunk-1*d2*opnochange*1\n" +
                "    :chunk\n" +
                "    (state <s1> ^operator <o1>)\n" +
                "    (<o1> ^name onc)\n" +
                "    -->\n" +
                "    (<s1> ^result true +)\n" +
                "}\n", false);
    }
    
    @Test(timeout=10000)
    public void testChunks2() throws Exception
    {
        runTest("testChunks2", -1);
    }
    
    @Test(timeout=10000)
    public void testChunks2WithLearning() throws Exception
    {
        agent.getInterpreter().eval("learn --on");
        runTest("testChunks2", -1);
    }
        
    @Test
    public void testNegatedConjunctiveChunkLoopBug510() throws Exception
    {
        runTest("testNegatedConjunctiveChunkLoopBug510", 3);
        assertEquals(3, agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue());
        assertEquals(5, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }

}
