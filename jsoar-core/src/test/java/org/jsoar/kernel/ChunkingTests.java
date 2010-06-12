/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jsoar.JSoarTest;
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
    
    @Test
    public void testBlocksWorldLookAheadWithMaxNoChangeBug() throws Exception
    {
        // This tests for a bug in the chunking caused by a bug in add_cond_to_tc()
        // where the id and attr test for positive conditions were added to the tc
        // rather than id and *value*. The first chunk constructed was incorrect
        runTest("testBlocksWorldLookAheadWithMaxNoChangeBug", 15);
        assertEquals(72, agent.getProductions().getProductions(ProductionType.DEFAULT).size());
        assertEquals(15, agent.getProductions().getProductions(ProductionType.USER).size());
        assertEquals(4, agent.getProductions().getProductions(ProductionType.CHUNK).size());
        
        // Make sure the chunk was built correctly.
        JSoarTest.verifyProduction(agent, 
                "chunk-1*d10*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-1*d10*opnochange*1\n" +
                "    :chunk\n" +
                "    (state <s1> ^operator <o1>)\n" +
                "    (<o1> -^default-desired-copy yes)\n" +
                "    (<o1> ^name evaluate-operator)\n" +
                "    (<o1> ^superproblem-space <s2>)\n" +
                "    (<s2> ^name move-blocks)\n" +
                "    (<o1> ^evaluation <e1>)\n" +
                "    (<s1> ^evaluation <e1>)\n" +
                "    (<o1> ^superstate <s3>)\n" +
                "    (<s3> ^name blocks-world)\n" +
                "    (<s3> ^object <o2>)\n" +
                "    (<o2> ^type block)\n" +
                "    (<e1> ^desired <d1>)\n" +
                "    (<o1> ^superoperator <s4>)\n" +
                "    (<s4> ^moving-block { <m1> <> <o2> })\n" +
                "    (<s3> ^object <m1>)\n" +
                "    (<s4> ^destination <d2>)\n" +
                "    (<s3> ^ontop <o3>)\n" +
                "    (<o3> ^top-block <o2>)\n" +
                "    (<o3> ^bottom-block { <b1> <> <d2> <> <m1> })\n" +
                "    (<s3> ^ontop <o4>)\n" +
                "    (<o4> ^top-block <m1>)\n" +
                "    (<o4> ^bottom-block <b1>)\n" +
                "    (<s3> ^ontop <o5>)\n" +
                "    (<o5> ^top-block <d2>)\n" +
                "    (<o5> ^bottom-block <b1>)\n" +
                "    (<d1> ^ontop <o6>)\n" +
                "    (<o6> ^top-block <o2>)\n" +
                "    (<o6> ^bottom-block <m1>)\n" +
                "    (<d1> ^ontop { <o7> <> <o6> })\n" +
                "    (<o7> ^top-block <m1>)\n" +
                "    (<o7> ^bottom-block <d2>)\n" +
                "    (<d1> ^ontop { <o8> <> <o7> <> <o6> })\n" +
                "    (<o8> ^top-block <d2>)\n" +
                "    (<o8> ^bottom-block <b1>)\n" +
                "    -->\n" +
                "    (<e1> ^symbolic-value success +)\n" +
                "}\n", true);

    }

}
