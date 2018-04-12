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
                "sp {chunk-1*d2*opnochange*1" + System.lineSeparator() + 
                "    :chunk" + System.lineSeparator() + 
                "    (state <s1> ^operator <o1>)" + System.lineSeparator() + 
                "    (<o1> ^name onc)" + System.lineSeparator() + 
                "    -->" + System.lineSeparator() + 
                "    (<s1> ^result true +)" + System.lineSeparator() + 
                "}" + System.lineSeparator(), false);
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
                "sp {chunk-1*d10*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> -^default-desired-copy yes)" + System.lineSeparator() + 
                        "    (<o1> ^name evaluate-operator)" + System.lineSeparator() + 
                        "    (<o1> ^superproblem-space <s2>)" + System.lineSeparator() + 
                        "    (<s2> ^name move-blocks)" + System.lineSeparator() + 
                        "    (<o1> ^evaluation <e1>)" + System.lineSeparator() + 
                        "    (<s1> ^evaluation <e1>)" + System.lineSeparator() + 
                        "    (<o1> ^superoperator <s3>)" + System.lineSeparator() + 
                        "    (<s3> ^name move-block)" + System.lineSeparator() + 
                        "    (<s3> ^moving-block <m1>)" + System.lineSeparator() + 
                        "    (<m1> ^type block)" + System.lineSeparator() + 
                        "    (<s3> ^destination { <d1> <> <m1> })" + System.lineSeparator() + 
                        "    (<d1> ^type block)" + System.lineSeparator() + 
                        "    (<o1> ^superstate <s4>)" + System.lineSeparator() + 
                        "    (<s4> ^name blocks-world)" + System.lineSeparator() + 
                        "    (<s4> ^object <m1>)" + System.lineSeparator() + 
                        "    (<s4> ^object <d1>)" + System.lineSeparator() + 
                        "    (<s4> ^object { <o2> <> <d1> <> <m1> })" + System.lineSeparator() + 
                        "    (<o2> ^type block)" + System.lineSeparator() + 
                        "    (<s4> ^object { <o3> <> <d1> <> <m1> })" + System.lineSeparator() + 
                        "    (<o3> ^type table)" + System.lineSeparator() + 
                        "    (<e1> ^desired <d2>)" + System.lineSeparator() + 
                        "    (<s4> ^ontop <o4>)" + System.lineSeparator() + 
                        "    (<o4> ^top-block <o2>)" + System.lineSeparator() + 
                        "    (<o4> ^bottom-block <o3>)" + System.lineSeparator() + 
                        "    (<s4> ^ontop <o5>)" + System.lineSeparator() + 
                        "    (<o5> ^top-block <m1>)" + System.lineSeparator() + 
                        "    (<o5> ^bottom-block <o3>)" + System.lineSeparator() + 
                        "    (<s4> ^ontop <o6>)" + System.lineSeparator() + 
                        "    (<o6> ^top-block <d1>)" + System.lineSeparator() + 
                        "    (<o6> ^bottom-block <o3>)" + System.lineSeparator() + 
                        "    (<d2> ^ontop <o7>)" + System.lineSeparator() + 
                        "    (<o7> ^top-block <o2>)" + System.lineSeparator() + 
                        "    (<o7> ^bottom-block <m1>)" + System.lineSeparator() + 
                        "    (<d2> ^ontop { <o8> <> <o7> })" + System.lineSeparator() + 
                        "    (<o8> ^top-block <m1>)" + System.lineSeparator() + 
                        "    (<o8> ^bottom-block <d1>)" + System.lineSeparator() + 
                        "    (<d2> ^ontop { <o9> <> <o8> <> <o7> })" + System.lineSeparator() + 
                        "    (<o9> ^top-block <d1>)" + System.lineSeparator() + 
                        "    (<o9> ^bottom-block <o3>)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<e1> ^symbolic-value success +)" + System.lineSeparator() + 
                        "}" + System.lineSeparator(), true);

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
                "sp {chunk-1*d5*opnochange*1" + System.lineSeparator() + 
                "    :chunk" + System.lineSeparator() + 
                "    (state <s1> ^numbers 5)" + System.lineSeparator() + 
                "    (<s1> ^numbers 4)" + System.lineSeparator() + 
                "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                "    (<o1> ^name do-test)" + System.lineSeparator() + 
                "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                "    (<t1> ^operators 5)" + System.lineSeparator() + 
                "    (<t1> ^prohibits 3)" + System.lineSeparator() + 
                "    (<t1> ^prohibits 1)" + System.lineSeparator() + 
                "    (<t1> ^rejects 5)" + System.lineSeparator() + 
                "    (<t1> ^worsts 2)" + System.lineSeparator() + 
                "    (<s1> ^numbers 3)" + System.lineSeparator() + 
                "    (<s1> ^numbers 2)" + System.lineSeparator() + 
                "    (<s1> ^numbers 1)" + System.lineSeparator() + 
                "    (<s1> ^worst-test 2)" + System.lineSeparator() + 
                "    (<s1> ^reject-test 5)" + System.lineSeparator() + 
                "    (<s1> ^prohibit-test 3)" + System.lineSeparator() + 
                "    (<s1> ^prohibit-test 1)" + System.lineSeparator() + 
                "    (<s1> ^acceptable-test 5)" + System.lineSeparator() + 
                "    (<s1> ^acceptable-test 4)" + System.lineSeparator() + 
                "    (<s1> ^acceptable-test 3)" + System.lineSeparator() + 
                "    (<s1> ^acceptable-test 2)" + System.lineSeparator() + 
                "    (<s1> ^acceptable-test 1)" + System.lineSeparator() + 
                "    -->" + System.lineSeparator() + 
                "    (<s1> ^result op4 +)" + System.lineSeparator() + 
                "}" +  System.lineSeparator(), true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-2*d11*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-2*d11*opnochange*1" + System.lineSeparator() + 
                    "    :chunk" + System.lineSeparator() + 
                    "    (state <s1> ^numbers 2)" + System.lineSeparator() + 
                    "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                    "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                    "    (<o1> ^name do-test)" + System.lineSeparator() + 
                    "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                    "    (<t1> ^operators 3)" + System.lineSeparator() + 
                    "    (<s1> ^nindifferent-test 2)" + System.lineSeparator() + 
                    "    (<s1> ^acceptable-test 2)" + System.lineSeparator() + 
                    "    (<t1> ^numerics <n1>)" + System.lineSeparator() + 
                    "    (<n1> ^referent 0.6)" + System.lineSeparator() + 
                    "    (<n1> ^value 2)" + System.lineSeparator() + 
                    "    -->" + System.lineSeparator() + 
                    "    (<s1> ^result op2 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-3*d17*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-3*d17*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^worse-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 1)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 3)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 3)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 2)" + System.lineSeparator() + 
                        "    (<s1> ^better-test 1)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 1)" + System.lineSeparator() + 
                        "    (<t1> ^worse <w1>)" + System.lineSeparator() + 
                        "    (<w1> ^referent 1)" + System.lineSeparator() + 
                        "    (<w1> ^value 3)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b1>)" + System.lineSeparator() + 
                        "    (<b1> ^referent 2)" + System.lineSeparator() + 
                        "    (<b1> ^value 1)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op1 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-4*d23*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-4*d23*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^numbers 2)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 23)" + System.lineSeparator() + 
                        "    (<t1> ^random-numerics 2)" + System.lineSeparator() + 
                        "    (<s1> ^nindifferent-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 2)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op2 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-5*d29*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-5*d29*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^numbers 1)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 6)" + System.lineSeparator() + 
                        "    (<t1> ^requires 1)" + System.lineSeparator() + 
                        "    (<s1> ^require-test 1)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 1)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op1 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-6*d35*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-6*d35*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^numbers 3)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 1)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 3)" + System.lineSeparator() + 
                        "    (<s1> ^nindifferent-test 1)" + System.lineSeparator() + 
                        "    (<s1> ^bindifferent-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 1)" + System.lineSeparator() + 
                        "    (<t1> ^binary-indifferents <b1>)" + System.lineSeparator() + 
                        "    (<b1> ^referent 1)" + System.lineSeparator() + 
                        "    (<b1> ^value 3)" + System.lineSeparator() + 
                        "    (<t1> ^numerics <n1>)" + System.lineSeparator() + 
                        "    (<n1> ^referent 0.5)" + System.lineSeparator() + 
                        "    (<n1> ^value 1)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op1 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-7*d41*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-7*d41*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^numbers 3)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 2)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 4)" + System.lineSeparator() + 
                        "    (<t1> ^bests 2)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^better-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 2)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b1>)" + System.lineSeparator() + 
                        "    (<b1> ^referent 3)" + System.lineSeparator() + 
                        "    (<b1> ^value 2)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op2 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-8*d47*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-8*d47*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^numbers 8)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 23)" + System.lineSeparator() + 
                        "    (<t1> ^unary-indifferents 8)" + System.lineSeparator() + 
                        "    (<s1> ^uindifferent-test 8)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 8)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op8 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);
        
        JSoarTest.verifyProduction(agent, 
                "chunk-9*d53*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-9*d53*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^numbers 23)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 11)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 23)" + System.lineSeparator() + 
                        "    (<t1> ^bests 23)" + System.lineSeparator() + 
                        "    (<t1> ^bests 22)" + System.lineSeparator() + 
                        "    (<t1> ^bests 21)" + System.lineSeparator() + 
                        "    (<t1> ^bests 20)" + System.lineSeparator() + 
                        "    (<t1> ^bests 19)" + System.lineSeparator() + 
                        "    (<t1> ^bests 18)" + System.lineSeparator() + 
                        "    (<t1> ^bests 17)" + System.lineSeparator() + 
                        "    (<t1> ^bests 16)" + System.lineSeparator() + 
                        "    (<t1> ^bests 15)" + System.lineSeparator() + 
                        "    (<t1> ^bests 14)" + System.lineSeparator() + 
                        "    (<t1> ^bests 13)" + System.lineSeparator() + 
                        "    (<t1> ^bests 12)" + System.lineSeparator() + 
                        "    (<t1> ^bests 11)" + System.lineSeparator() + 
                        "    (<t1> ^bests 10)" + System.lineSeparator() + 
                        "    (<t1> ^bests 9)" + System.lineSeparator() + 
                        "    (<t1> ^bests 8)" + System.lineSeparator() + 
                        "    (<t1> ^bests 7)" + System.lineSeparator() + 
                        "    (<t1> ^bests 6)" + System.lineSeparator() + 
                        "    (<t1> ^bests 5)" + System.lineSeparator() + 
                        "    (<t1> ^bests 4)" + System.lineSeparator() + 
                        "    (<t1> ^bests 3)" + System.lineSeparator() + 
                        "    (<t1> ^bests 2)" + System.lineSeparator() + 
                        "    (<t1> ^bests 1)" + System.lineSeparator() + 
                        "    (<t1> ^random-numerics 11)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 22)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 21)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 20)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 19)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 18)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 17)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 16)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 15)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 14)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 13)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 12)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 10)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 9)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 8)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 7)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 6)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 5)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 4)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 3)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 2)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 1)" + System.lineSeparator() + 
                        "    (<s1> ^nindifferent-test 11)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 23)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 22)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 21)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 20)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 19)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 18)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 17)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 16)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 15)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 14)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 13)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 12)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 11)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 10)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 9)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 8)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 7)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 6)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 5)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 4)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^best-test 1)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 23)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 22)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 21)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 20)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 19)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 18)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 17)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 16)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 15)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 14)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 13)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 12)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 11)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 10)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 9)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 8)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 7)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 6)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 5)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 4)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 1)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op11 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);

        JSoarTest.verifyProduction(agent, 
                "chunk-10*d59*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-10*d59*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^numbers 23)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 1)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 23)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 22)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 21)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 20)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 19)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 18)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 17)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 16)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 15)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 14)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 13)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 12)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 11)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 10)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 9)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 8)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 7)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 6)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 5)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 4)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 3)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 2)" + System.lineSeparator() + 
                        "    (<s1> ^better-test 1)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 23)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 22)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 21)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 20)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 19)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 18)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 17)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 16)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 15)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 14)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 13)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 12)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 11)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 10)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 9)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 8)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 7)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 6)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 5)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 4)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b1>)" + System.lineSeparator() + 
                        "    (<b1> ^referent 23)" + System.lineSeparator() + 
                        "    (<b1> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b2>)" + System.lineSeparator() + 
                        "    (<b2> ^referent 22)" + System.lineSeparator() + 
                        "    (<b2> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b3>)" + System.lineSeparator() + 
                        "    (<b3> ^referent 21)" + System.lineSeparator() + 
                        "    (<b3> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b4>)" + System.lineSeparator() + 
                        "    (<b4> ^referent 20)" + System.lineSeparator() + 
                        "    (<b4> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b5>)" + System.lineSeparator() + 
                        "    (<b5> ^referent 19)" + System.lineSeparator() + 
                        "    (<b5> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b6>)" + System.lineSeparator() + 
                        "    (<b6> ^referent 18)" + System.lineSeparator() + 
                        "    (<b6> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b7>)" + System.lineSeparator() + 
                        "    (<b7> ^referent 17)" + System.lineSeparator() + 
                        "    (<b7> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b8>)" + System.lineSeparator() + 
                        "    (<b8> ^referent 16)" + System.lineSeparator() + 
                        "    (<b8> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b9>)" + System.lineSeparator() + 
                        "    (<b9> ^referent 15)" + System.lineSeparator() + 
                        "    (<b9> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b10>)" + System.lineSeparator() + 
                        "    (<b10> ^referent 14)" + System.lineSeparator() + 
                        "    (<b10> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b11>)" + System.lineSeparator() + 
                        "    (<b11> ^referent 13)" + System.lineSeparator() + 
                        "    (<b11> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b12>)" + System.lineSeparator() + 
                        "    (<b12> ^referent 12)" + System.lineSeparator() + 
                        "    (<b12> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b13>)" + System.lineSeparator() + 
                        "    (<b13> ^referent 11)" + System.lineSeparator() + 
                        "    (<b13> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b14>)" + System.lineSeparator() + 
                        "    (<b14> ^referent 10)" + System.lineSeparator() + 
                        "    (<b14> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b15>)" + System.lineSeparator() + 
                        "    (<b15> ^referent 9)" + System.lineSeparator() + 
                        "    (<b15> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b16>)" + System.lineSeparator() + 
                        "    (<b16> ^referent 8)" + System.lineSeparator() + 
                        "    (<b16> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b17>)" + System.lineSeparator() + 
                        "    (<b17> ^referent 7)" + System.lineSeparator() + 
                        "    (<b17> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b18>)" + System.lineSeparator() + 
                        "    (<b18> ^referent 6)" + System.lineSeparator() + 
                        "    (<b18> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b19>)" + System.lineSeparator() + 
                        "    (<b19> ^referent 5)" + System.lineSeparator() + 
                        "    (<b19> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b20>)" + System.lineSeparator() + 
                        "    (<b20> ^referent 4)" + System.lineSeparator() + 
                        "    (<b20> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b21>)" + System.lineSeparator() + 
                        "    (<b21> ^referent 3)" + System.lineSeparator() + 
                        "    (<b21> ^value 1)" + System.lineSeparator() + 
                        "    (<t1> ^betters <b22>)" + System.lineSeparator() + 
                        "    (<b22> ^referent 2)" + System.lineSeparator() + 
                        "    (<b22> ^value 1)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op1 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);

        JSoarTest.verifyProduction(agent, 
                "chunk-11*d65*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-11*d65*opnochange*1" + System.lineSeparator() + 
                        "    :chunk" + System.lineSeparator() + 
                        "    (state <s1> ^numbers 5)" + System.lineSeparator() + 
                        "    (<s1> ^superstate nil)" + System.lineSeparator() + 
                        "    (<s1> ^operator <o1>)" + System.lineSeparator() + 
                        "    (<o1> ^name do-test)" + System.lineSeparator() + 
                        "    (<o1> ^test <t1>)" + System.lineSeparator() + 
                        "    (<t1> ^operators 5)" + System.lineSeparator() + 
                        "    (<t1> ^prohibits 1)" + System.lineSeparator() + 
                        "    (<t1> ^worsts 3)" + System.lineSeparator() + 
                        "    (<t1> ^worsts 2)" + System.lineSeparator() + 
                        "    (<t1> ^unary-indifferents 5)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 3)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 2)" + System.lineSeparator() + 
                        "    (<s1> ^numbers 1)" + System.lineSeparator() + 
                        "    (<s1> ^uindifferent-test 5)" + System.lineSeparator() + 
                        "    (<s1> ^worst-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^worst-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^prohibit-test 1)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 5)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 3)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 2)" + System.lineSeparator() + 
                        "    (<s1> ^acceptable-test 1)" + System.lineSeparator() + 
                        "    -->" + System.lineSeparator() + 
                        "    (<s1> ^result op5 +)" + System.lineSeparator() + 
                    "}" + System.lineSeparator(), true);
    }

}
