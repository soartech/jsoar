/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2010
 */
package org.jsoar.kernel.smem;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.tracing.Printer;
import org.junit.Test;

/**
 * @author ray
 */
public class SMemFunctionalTests extends FunctionalTestHarness
{
    @Test
    public void testSimpleCueBasedRetrieval() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleCueBasedRetrieval", 1);
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleNonCueBasedRetrieval", 2);
    }
    
    @Test
    public void testSimpleStore() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleStore", 2);
    }
    
    @Test
    public void testMirroring() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMirroring", 4);
    }
    
    @Test
    public void testMergeAdd() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMergeAdd", 4);
    }
    
    @Test
    public void testMergeNone() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMergeNone", 4);
    }
    
    @Test
    public void testSimpleStoreMultivaluedAttribute() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleStoreMultivaluedAttribute", 2);
    }
    
    @Test
    public void testSimpleStoreGC() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTestSetup("testSimpleStoreGC");
        
        agent.runFor(2, RunType.DECISIONS);
        
        String result = agent.getInterpreter().eval("smem --print");
        
        String[] split = result.split("\\s+");
        
        List<String> ltis = new ArrayList<String>();
        
        for (String lti : split)
        {
            if (lti.length() == 3 ||
                lti.length() == 4)
            {
                if (lti.charAt(0) == '@')
                {
                    ltis.add(lti);
                }
                else if (lti.charAt(0) == '(' &&
                         lti.charAt(1) == '@')
                {
                    ltis.add(lti.substring(1));
                }
            }
        }
        
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        
        agent.runFor(4, RunType.DECISIONS);
        
        StringWriter outputWriter;
        agent.getPrinter().addPersistentWriter(
                outputWriter = new StringWriter());
        
        agent.getInterpreter().eval("print (* ^retrieve *)");
        
        String result_after = outputWriter.toString();
        
        String[] split_after = result_after.split("\\s+");
        
        for (String lti : split_after)
        {
            if ((lti.length() == 3 || lti.length() == 4) && lti.charAt(0) == '@')
            {
                String correctLti = lti.substring(0, 3);
                assertTrue(ltis.contains(correctLti));
            }
        }
    }
}
