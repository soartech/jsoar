/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import org.jsoar.JSoarTest;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class ChunkingTests extends FunctionalTestHarness
{    
    public void testJustifications() throws Exception
    {
        runTest("testJustifications", 2);
        Production j = agent.getProductions().getProduction("justification-1");
        assertNull(j);
    }
    
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
    
    public void testChunks2() throws Exception
    {
        runTest("testChunks2", -1);
    }
    
    public void testChunks2WithLearning() throws Exception
    {
        agent.getInterpreter().eval("learn --on");
        runTest("testChunks2", -1);
    }
        
    public void testNegatedConjunctiveChunkLoopBug510() throws Exception
    {
        runTest("testNegatedConjunctiveChunkLoopBug510", 3);
        assertEquals(3, agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue());
        assertEquals(5, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }
    
    public void testBlocksWorldLookAheadWithMaxNoChangeBug() throws Exception
    {
        // This tests for a bug in the chunking caused by a bug in add_cond_to_tc()
        // where the id and attr test for positive conditions were added to the tc
        // rather than id and *value*. The first chunk constructed was incorrect
        Chunker chunker = Adaptables.adapt(agent, Chunker.class);
        chunker.chunkThroughEvaluationRules = true;
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
                        "    (<o1> ^superoperator <s3>)\n" +
                        "    (<s3> ^name move-block)\n" +
                        "    (<s3> ^moving-block <m1>)\n" +
                        "    (<m1> ^type block)\n" +
                        "    (<s3> ^destination { <d1> <> <m1> })\n" +
                        "    (<d1> ^type block)\n" +
                        "    (<o1> ^superstate <s4>)\n" +
                        "    (<s4> ^name blocks-world)\n" +
                        "    (<s4> ^object <m1>)\n" +
                        "    (<s4> ^object <d1>)\n" +
                        "    (<s4> ^object { <o2> <> <d1> <> <m1> })\n" +
                        "    (<o2> ^type block)\n" +
                        "    (<s4> ^object { <o3> <> <d1> <> <m1> })\n" +
                        "    (<o3> ^type table)\n" +
                        "    (<e1> ^desired <d2>)\n" +
                        "    (<s4> ^ontop <o4>)\n" +
                        "    (<o4> ^top-block <o2>)\n" +
                        "    (<o4> ^bottom-block <o3>)\n" +
                        "    (<s4> ^ontop <o5>)\n" +
                        "    (<o5> ^top-block <m1>)\n" +
                        "    (<o5> ^bottom-block <o3>)\n" +
                        "    (<s4> ^ontop <o6>)\n" +
                        "    (<o6> ^top-block <d1>)\n" +
                        "    (<o6> ^bottom-block <o3>)\n" +
                        "    (<d2> ^ontop <o7>)\n" +
                        "    (<o7> ^top-block <o2>)\n" +
                        "    (<o7> ^bottom-block <m1>)\n" +
                        "    (<d2> ^ontop { <o8> <> <o7> })\n" +
                        "    (<o8> ^top-block <m1>)\n" +
                        "    (<o8> ^bottom-block <d1>)\n" +
                        "    (<d2> ^ontop { <o9> <> <o8> <> <o7> })\n" +
                        "    (<o9> ^top-block <d1>)\n" +
                        "    (<o9> ^bottom-block <o3>)\n" +
                        "    -->\n" +
                        "    (<e1> ^symbolic-value success +)\n" +
                        "}\n", true);

    }

    public void testCDPS() throws Exception
    {
        Chunker chunker = Adaptables.adapt(agent, Chunker.class);
        chunker.chunkThroughEvaluationRules = true;
        runTest("testCDPS", -1);
        assertEquals(11, agent.getProductions().getProductions(ProductionType.CHUNK).size());
        
        // Make sure the chunks were built correctly.
        
        JSoarTest.verifyProduction(agent, 
                "chunk-1*d5*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-1*d5*opnochange*1\n" +
                "    :chunk\n" +
                "    (state <s1> ^numbers 5)\n" +
                "    (<s1> ^numbers 4)\n" +
                "    (<s1> ^superstate nil)\n" +
                "    (<s1> ^operator <o1>)\n" +
                "    (<o1> ^name do-test)\n" +
                "    (<o1> ^test <t1>)\n" +
                "    (<t1> ^operators 5)\n" +
                "    (<t1> ^prohibits 3)\n" +
                "    (<t1> ^prohibits 1)\n" +
                "    (<t1> ^rejects 5)\n" +
                "    (<t1> ^worsts 2)\n" +
                "    (<s1> ^numbers 3)\n" +
                "    (<s1> ^numbers 2)\n" +
                "    (<s1> ^numbers 1)\n" +
                "    (<s1> ^worst-test 2)\n" +
                "    (<s1> ^reject-test 5)\n" +
                "    (<s1> ^prohibit-test 3)\n" +
                "    (<s1> ^prohibit-test 1)\n" +
                "    (<s1> ^acceptable-test 5)\n" +
                "    (<s1> ^acceptable-test 4)\n" +
                "    (<s1> ^acceptable-test 3)\n" +
                "    (<s1> ^acceptable-test 2)\n" +
                "    (<s1> ^acceptable-test 1)\n" +
                "    -->\n" +
                "    (<s1> ^result op4 +)\n" +
                "}\n", true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-2*d11*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-2*d11*opnochange*1\n" +
                    "    :chunk\n" +
                    "    (state <s1> ^numbers 2)\n" +
                    "    (<s1> ^superstate nil)\n" +
                    "    (<s1> ^operator <o1>)\n" +
                    "    (<o1> ^name do-test)\n" +
                    "    (<o1> ^test <t1>)\n" +
                    "    (<t1> ^operators 3)\n" +
                    "    (<s1> ^nindifferent-test 2)\n" +
                    "    (<s1> ^acceptable-test 2)\n" +
                    "    (<t1> ^numerics <n1>)\n" +
                    "    (<n1> ^referent 0.6)\n" +
                    "    (<n1> ^value 2)\n" +
                    "    -->\n" +
                    "    (<s1> ^result op2 +)\n" +
                    "}\n", true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-3*d17*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-3*d17*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^worse-test 3)\n" +
                        "    (<s1> ^numbers 1)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 3)\n" +
                        "    (<s1> ^numbers 3)\n" +
                        "    (<s1> ^numbers 2)\n" +
                        "    (<s1> ^better-test 1)\n" +
                        "    (<s1> ^acceptable-test 3)\n" +
                        "    (<s1> ^acceptable-test 2)\n" +
                        "    (<s1> ^acceptable-test 1)\n" +
                        "    (<t1> ^worse <w1>)\n" +
                        "    (<w1> ^referent 1)\n" +
                        "    (<w1> ^value 3)\n" +
                        "    (<t1> ^betters <b1>)\n" +
                        "    (<b1> ^referent 2)\n" +
                        "    (<b1> ^value 1)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op1 +)\n" +
                    "}\n", true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-4*d23*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-4*d23*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^numbers 2)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 23)\n" +
                        "    (<t1> ^random-numerics 2)\n" +
                        "    (<s1> ^nindifferent-test 2)\n" +
                        "    (<s1> ^acceptable-test 2)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op2 +)\n" +
                    "}\n", true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-5*d29*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-5*d29*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^numbers 1)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 6)\n" +
                        "    (<t1> ^requires 1)\n" +
                        "    (<s1> ^require-test 1)\n" +
                        "    (<s1> ^acceptable-test 1)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op1 +)\n" +
                    "}\n", true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-6*d35*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-6*d35*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^numbers 3)\n" +
                        "    (<s1> ^numbers 1)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 3)\n" +
                        "    (<s1> ^nindifferent-test 1)\n" +
                        "    (<s1> ^bindifferent-test 3)\n" +
                        "    (<s1> ^acceptable-test 3)\n" +
                        "    (<s1> ^acceptable-test 1)\n" +
                        "    (<t1> ^binary-indifferents <b1>)\n" +
                        "    (<b1> ^referent 1)\n" +
                        "    (<b1> ^value 3)\n" +
                        "    (<t1> ^numerics <n1>)\n" +
                        "    (<n1> ^referent 0.5)\n" +
                        "    (<n1> ^value 1)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op1 +)\n" +
                    "}\n", true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-7*d41*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-7*d41*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^numbers 3)\n" +
                        "    (<s1> ^numbers 2)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 4)\n" +
                        "    (<t1> ^bests 2)\n" +
                        "    (<s1> ^best-test 2)\n" +
                        "    (<s1> ^better-test 2)\n" +
                        "    (<s1> ^acceptable-test 3)\n" +
                        "    (<s1> ^acceptable-test 2)\n" +
                        "    (<t1> ^betters <b1>)\n" +
                        "    (<b1> ^referent 3)\n" +
                        "    (<b1> ^value 2)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op2 +)\n" +
                    "}\n", true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-8*d47*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-8*d47*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^numbers 8)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 23)\n" +
                        "    (<t1> ^unary-indifferents 8)\n" +
                        "    (<s1> ^uindifferent-test 8)\n" +
                        "    (<s1> ^acceptable-test 8)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op8 +)\n" +
                    "}\n", true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-9*d53*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-9*d53*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^numbers 23)\n" +
                        "    (<s1> ^numbers 11)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 23)\n" +
                        "    (<t1> ^bests 23)\n" +
                        "    (<t1> ^bests 22)\n" +
                        "    (<t1> ^bests 21)\n" +
                        "    (<t1> ^bests 20)\n" +
                        "    (<t1> ^bests 19)\n" +
                        "    (<t1> ^bests 18)\n" +
                        "    (<t1> ^bests 17)\n" +
                        "    (<t1> ^bests 16)\n" +
                        "    (<t1> ^bests 15)\n" +
                        "    (<t1> ^bests 14)\n" +
                        "    (<t1> ^bests 13)\n" +
                        "    (<t1> ^bests 12)\n" +
                        "    (<t1> ^bests 11)\n" +
                        "    (<t1> ^bests 10)\n" +
                        "    (<t1> ^bests 9)\n" +
                        "    (<t1> ^bests 8)\n" +
                        "    (<t1> ^bests 7)\n" +
                        "    (<t1> ^bests 6)\n" +
                        "    (<t1> ^bests 5)\n" +
                        "    (<t1> ^bests 4)\n" +
                        "    (<t1> ^bests 3)\n" +
                        "    (<t1> ^bests 2)\n" +
                        "    (<t1> ^bests 1)\n" +
                        "    (<t1> ^random-numerics 11)\n" +
                        "    (<s1> ^numbers 22)\n" +
                        "    (<s1> ^numbers 21)\n" +
                        "    (<s1> ^numbers 20)\n" +
                        "    (<s1> ^numbers 19)\n" +
                        "    (<s1> ^numbers 18)\n" +
                        "    (<s1> ^numbers 17)\n" +
                        "    (<s1> ^numbers 16)\n" +
                        "    (<s1> ^numbers 15)\n" +
                        "    (<s1> ^numbers 14)\n" +
                        "    (<s1> ^numbers 13)\n" +
                        "    (<s1> ^numbers 12)\n" +
                        "    (<s1> ^numbers 10)\n" +
                        "    (<s1> ^numbers 9)\n" +
                        "    (<s1> ^numbers 8)\n" +
                        "    (<s1> ^numbers 7)\n" +
                        "    (<s1> ^numbers 6)\n" +
                        "    (<s1> ^numbers 5)\n" +
                        "    (<s1> ^numbers 4)\n" +
                        "    (<s1> ^numbers 3)\n" +
                        "    (<s1> ^numbers 2)\n" +
                        "    (<s1> ^numbers 1)\n" +
                        "    (<s1> ^nindifferent-test 11)\n" +
                        "    (<s1> ^best-test 23)\n" +
                        "    (<s1> ^best-test 22)\n" +
                        "    (<s1> ^best-test 21)\n" +
                        "    (<s1> ^best-test 20)\n" +
                        "    (<s1> ^best-test 19)\n" +
                        "    (<s1> ^best-test 18)\n" +
                        "    (<s1> ^best-test 17)\n" +
                        "    (<s1> ^best-test 16)\n" +
                        "    (<s1> ^best-test 15)\n" +
                        "    (<s1> ^best-test 14)\n" +
                        "    (<s1> ^best-test 13)\n" +
                        "    (<s1> ^best-test 12)\n" +
                        "    (<s1> ^best-test 11)\n" +
                        "    (<s1> ^best-test 10)\n" +
                        "    (<s1> ^best-test 9)\n" +
                        "    (<s1> ^best-test 8)\n" +
                        "    (<s1> ^best-test 7)\n" +
                        "    (<s1> ^best-test 6)\n" +
                        "    (<s1> ^best-test 5)\n" +
                        "    (<s1> ^best-test 4)\n" +
                        "    (<s1> ^best-test 3)\n" +
                        "    (<s1> ^best-test 2)\n" +
                        "    (<s1> ^best-test 1)\n" +
                        "    (<s1> ^acceptable-test 23)\n" +
                        "    (<s1> ^acceptable-test 22)\n" +
                        "    (<s1> ^acceptable-test 21)\n" +
                        "    (<s1> ^acceptable-test 20)\n" +
                        "    (<s1> ^acceptable-test 19)\n" +
                        "    (<s1> ^acceptable-test 18)\n" +
                        "    (<s1> ^acceptable-test 17)\n" +
                        "    (<s1> ^acceptable-test 16)\n" +
                        "    (<s1> ^acceptable-test 15)\n" +
                        "    (<s1> ^acceptable-test 14)\n" +
                        "    (<s1> ^acceptable-test 13)\n" +
                        "    (<s1> ^acceptable-test 12)\n" +
                        "    (<s1> ^acceptable-test 11)\n" +
                        "    (<s1> ^acceptable-test 10)\n" +
                        "    (<s1> ^acceptable-test 9)\n" +
                        "    (<s1> ^acceptable-test 8)\n" +
                        "    (<s1> ^acceptable-test 7)\n" +
                        "    (<s1> ^acceptable-test 6)\n" +
                        "    (<s1> ^acceptable-test 5)\n" +
                        "    (<s1> ^acceptable-test 4)\n" +
                        "    (<s1> ^acceptable-test 3)\n" +
                        "    (<s1> ^acceptable-test 2)\n" +
                        "    (<s1> ^acceptable-test 1)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op11 +)\n" +
                    "}\n", true);

        JSoarTest.verifyProduction(agent, 
                "chunk-10*d59*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-10*d59*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^numbers 23)\n" +
                        "    (<s1> ^numbers 1)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 23)\n" +
                        "    (<s1> ^numbers 22)\n" +
                        "    (<s1> ^numbers 21)\n" +
                        "    (<s1> ^numbers 20)\n" +
                        "    (<s1> ^numbers 19)\n" +
                        "    (<s1> ^numbers 18)\n" +
                        "    (<s1> ^numbers 17)\n" +
                        "    (<s1> ^numbers 16)\n" +
                        "    (<s1> ^numbers 15)\n" +
                        "    (<s1> ^numbers 14)\n" +
                        "    (<s1> ^numbers 13)\n" +
                        "    (<s1> ^numbers 12)\n" +
                        "    (<s1> ^numbers 11)\n" +
                        "    (<s1> ^numbers 10)\n" +
                        "    (<s1> ^numbers 9)\n" +
                        "    (<s1> ^numbers 8)\n" +
                        "    (<s1> ^numbers 7)\n" +
                        "    (<s1> ^numbers 6)\n" +
                        "    (<s1> ^numbers 5)\n" +
                        "    (<s1> ^numbers 4)\n" +
                        "    (<s1> ^numbers 3)\n" +
                        "    (<s1> ^numbers 2)\n" +
                        "    (<s1> ^better-test 1)\n" +
                        "    (<s1> ^acceptable-test 23)\n" +
                        "    (<s1> ^acceptable-test 22)\n" +
                        "    (<s1> ^acceptable-test 21)\n" +
                        "    (<s1> ^acceptable-test 20)\n" +
                        "    (<s1> ^acceptable-test 19)\n" +
                        "    (<s1> ^acceptable-test 18)\n" +
                        "    (<s1> ^acceptable-test 17)\n" +
                        "    (<s1> ^acceptable-test 16)\n" +
                        "    (<s1> ^acceptable-test 15)\n" +
                        "    (<s1> ^acceptable-test 14)\n" +
                        "    (<s1> ^acceptable-test 13)\n" +
                        "    (<s1> ^acceptable-test 12)\n" +
                        "    (<s1> ^acceptable-test 11)\n" +
                        "    (<s1> ^acceptable-test 10)\n" +
                        "    (<s1> ^acceptable-test 9)\n" +
                        "    (<s1> ^acceptable-test 8)\n" +
                        "    (<s1> ^acceptable-test 7)\n" +
                        "    (<s1> ^acceptable-test 6)\n" +
                        "    (<s1> ^acceptable-test 5)\n" +
                        "    (<s1> ^acceptable-test 4)\n" +
                        "    (<s1> ^acceptable-test 3)\n" +
                        "    (<s1> ^acceptable-test 2)\n" +
                        "    (<s1> ^acceptable-test 1)\n" +
                        "    (<t1> ^betters <b1>)\n" +
                        "    (<b1> ^referent 23)\n" +
                        "    (<b1> ^value 1)\n" +
                        "    (<t1> ^betters <b2>)\n" +
                        "    (<b2> ^referent 22)\n" +
                        "    (<b2> ^value 1)\n" +
                        "    (<t1> ^betters <b3>)\n" +
                        "    (<b3> ^referent 21)\n" +
                        "    (<b3> ^value 1)\n" +
                        "    (<t1> ^betters <b4>)\n" +
                        "    (<b4> ^referent 20)\n" +
                        "    (<b4> ^value 1)\n" +
                        "    (<t1> ^betters <b5>)\n" +
                        "    (<b5> ^referent 19)\n" +
                        "    (<b5> ^value 1)\n" +
                        "    (<t1> ^betters <b6>)\n" +
                        "    (<b6> ^referent 18)\n" +
                        "    (<b6> ^value 1)\n" +
                        "    (<t1> ^betters <b7>)\n" +
                        "    (<b7> ^referent 17)\n" +
                        "    (<b7> ^value 1)\n" +
                        "    (<t1> ^betters <b8>)\n" +
                        "    (<b8> ^referent 16)\n" +
                        "    (<b8> ^value 1)\n" +
                        "    (<t1> ^betters <b9>)\n" +
                        "    (<b9> ^referent 15)\n" +
                        "    (<b9> ^value 1)\n" +
                        "    (<t1> ^betters <b10>)\n" +
                        "    (<b10> ^referent 14)\n" +
                        "    (<b10> ^value 1)\n" +
                        "    (<t1> ^betters <b11>)\n" +
                        "    (<b11> ^referent 13)\n" +
                        "    (<b11> ^value 1)\n" +
                        "    (<t1> ^betters <b12>)\n" +
                        "    (<b12> ^referent 12)\n" +
                        "    (<b12> ^value 1)\n" +
                        "    (<t1> ^betters <b13>)\n" +
                        "    (<b13> ^referent 11)\n" +
                        "    (<b13> ^value 1)\n" +
                        "    (<t1> ^betters <b14>)\n" +
                        "    (<b14> ^referent 10)\n" +
                        "    (<b14> ^value 1)\n" +
                        "    (<t1> ^betters <b15>)\n" +
                        "    (<b15> ^referent 9)\n" +
                        "    (<b15> ^value 1)\n" +
                        "    (<t1> ^betters <b16>)\n" +
                        "    (<b16> ^referent 8)\n" +
                        "    (<b16> ^value 1)\n" +
                        "    (<t1> ^betters <b17>)\n" +
                        "    (<b17> ^referent 7)\n" +
                        "    (<b17> ^value 1)\n" +
                        "    (<t1> ^betters <b18>)\n" +
                        "    (<b18> ^referent 6)\n" +
                        "    (<b18> ^value 1)\n" +
                        "    (<t1> ^betters <b19>)\n" +
                        "    (<b19> ^referent 5)\n" +
                        "    (<b19> ^value 1)\n" +
                        "    (<t1> ^betters <b20>)\n" +
                        "    (<b20> ^referent 4)\n" +
                        "    (<b20> ^value 1)\n" +
                        "    (<t1> ^betters <b21>)\n" +
                        "    (<b21> ^referent 3)\n" +
                        "    (<b21> ^value 1)\n" +
                        "    (<t1> ^betters <b22>)\n" +
                        "    (<b22> ^referent 2)\n" +
                        "    (<b22> ^value 1)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op1 +)\n" +
                    "}\n", true);

        JSoarTest.verifyProduction(agent, 
                "chunk-11*d65*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-11*d65*opnochange*1\n" +
                        "    :chunk\n" +
                        "    (state <s1> ^numbers 5)\n" +
                        "    (<s1> ^superstate nil)\n" +
                        "    (<s1> ^operator <o1>)\n" +
                        "    (<o1> ^name do-test)\n" +
                        "    (<o1> ^test <t1>)\n" +
                        "    (<t1> ^operators 5)\n" +
                        "    (<t1> ^prohibits 1)\n" +
                        "    (<t1> ^worsts 3)\n" +
                        "    (<t1> ^worsts 2)\n" +
                        "    (<t1> ^unary-indifferents 5)\n" +
                        "    (<s1> ^numbers 3)\n" +
                        "    (<s1> ^numbers 2)\n" +
                        "    (<s1> ^numbers 1)\n" +
                        "    (<s1> ^uindifferent-test 5)\n" +
                        "    (<s1> ^worst-test 3)\n" +
                        "    (<s1> ^worst-test 2)\n" +
                        "    (<s1> ^prohibit-test 1)\n" +
                        "    (<s1> ^acceptable-test 5)\n" +
                        "    (<s1> ^acceptable-test 3)\n" +
                        "    (<s1> ^acceptable-test 2)\n" +
                        "    (<s1> ^acceptable-test 1)\n" +
                        "    -->\n" +
                        "    (<s1> ^result op5 +)\n" +
                    "}\n", true);
    }

}
