/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author ray
 */
public class RLTests extends FunctionalTestHarness
{
    private static final double tolerance = 0.0000001;

    @Test
    public void testTemplateVariableNameBug1121() throws Exception
    {
        runTest("testTemplateVariableNameBug1121", 1);
        assertEquals(4, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }
    
    @Test
    public void testRLUnit() throws Exception
    {
        runTest("testRLUnit", 25);
        
//        rl*value*function*1 1.000000  19.999961853027344
//        rl*value*function*2 1.000000  10.0
//        rl*value*function*3 1.000000  10.0
//        rl*value*function*4 1.000000  15.0
//        rl*value*function*5 1.000000  10.0
//        rl*value*function*6 1.000000  15.0
//        rl*value*function*7 1.000000  10.0
//        rl*value*function*8 1.000000  18.75
//        rl*value*function*9 1.000000  10.0

        double expectedValues[] = {19.999961853027344, 10.0, 10.0, 15.0, 10.0, 15.0, 10.0, 18.75, 10.0};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//        rl*value*function*1 2.000000  37.99992752075195
//        rl*value*function*2 2.000000  19.5
//        rl*value*function*3 2.000000  19.0
//        rl*value*function*4 2.000000  28.75
//        rl*value*function*5 2.000000  19.0
//        rl*value*function*6 2.000000  28.75
//        rl*value*function*7 2.000000  19.0
//        rl*value*function*8 2.000000  35.6875
//        rl*value*function*9 2.000000  19.0

        double expectedValues2[] = {37.99992752075195, 19.5, 19.0, 28.75, 19.0, 28.75, 19.0, 35.6875, 19.0};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues2));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//        rl*value*function*1 3.000000  54.1998966217041
//        rl*value*function*2 3.000000  28.5
//        rl*value*function*3 3.000000  27.1
//        rl*value*function*4 3.000000  41.35
//        rl*value*function*5 3.000000  27.1
//        rl*value*function*6 3.000000  41.35
//        rl*value*function*7 3.000000  27.1
//        rl*value*function*8 3.000000  50.9875
//        rl*value*function*9 3.000000  27.1
        
        double expectedValues3[] = {54.1998966217041, 28.5, 27.1, 41.35, 27.1, 41.35, 27.1, 50.9875, 27.1};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues3));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//        rl*value*function*1 4.000000  68.77986881256103
//        rl*value*function*2 4.000000  37.005
//        rl*value*function*3 4.000000  34.39
//        rl*value*function*4 4.000000  52.8925
//        rl*value*function*5 4.000000  34.39
//        rl*value*function*6 4.000000  52.8925
//        rl*value*function*7 4.000000  34.39
//        rl*value*function*8 4.000000  64.808125
//        rl*value*function*9 4.000000  34.39

        double expectedValues4[] = {68.77986881256103, 37.005, 34.39, 52.8925, 34.39, 52.8925, 34.39, 64.808125, 34.39};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues4));

    }
    
    /*
     * Check expected RL values against actual RL values for a set of RL rules that share a common prefix
     * This assumes that the RL rules to be checked are named in this form: [prefix][integer] 
     * E.g., my*rl*rule*1, my*rl*rule*2, etc. where prefix = "my*rl*rule*" (template rules generate RL rules in this form)
     * The expected values are provided in an array ordered so the indexes match up with the rule numbers
     * E.g., my*rl*rule*1's expected value is at index 0
     * Performs an inexact match on double values (tests values within small tolerance).
     * Returns true if matches, false if not
     */
    private boolean checkExpectedValues(String rulePrefix, double[] expectedValues)
    {
        boolean result = true;
        for(Production p : agent.getProductions().getProductions(null))
        {
            if(p.rl_rule && p.getName().startsWith(rulePrefix))
            {
                int startOfIndex = p.getName().lastIndexOf('*') + 1;
                int index = Integer.valueOf(p.getName().substring(startOfIndex)) - 1;
                double value = Double.valueOf(p.action_list.asMakeAction().referent.toString());
                
                if(Math.abs(expectedValues[index] - value) > tolerance)
                {
                    result = false;
                    break;
                }
            }
        }
        
        return result;
    }
}
