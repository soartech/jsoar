/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *  
 * Created on Sep 19, 2008
 *  
 *  Updated by PL, 28 August 2013
 *
 *	This test runs some simple Soar code that is found in
 *	/org/jsoar/kernel/RLTests_testRLUnit.soar and then
 *	checks the numerical results.
 *
 *  Two possible sets of numbers are checked, depending
 *  on whether the hrl-discount parameter is on or off.
 *  Two tests here set the parameter first and then run
 *  and check the resulting numbers.
 *  
 *  This test is somewhat complementary to the one in
 *  org.jsoar.kernel.learning.ReinforcementLearningTest.java,
 *  which uses the same .soar file.
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jsoar.kernel.learning.rl.ReinforcementLearningParams;
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

/*
 * CORRECTIONS:		PL 8/13/2013
 * 
 * The expected values used below are based on having the hrl-discount
 * parameter set to the current JSoar default of true.
 * 
 * However, the latest CSoar has this parameter set to off (false).
 * 
 * The code below now has two sets of values, and chooses which one
 * to use based on the current value of hrl-discount.
 * 
 * This way the tests will pass regardless of what
 * default is set up in JSoar.
 * 
 */
    @Test
    public void testRLUnitHrlDiscountOn() throws Exception
    {
    	//	Set hrl-discount on
        agent.getProperties()
        		.set(ReinforcementLearningParams.HRL_DISCOUNT,
        				ReinforcementLearningParams.HrlDiscount.on);

        runTest("testRLUnit", 25);
//			Value of hrl-discount:		off					on        
//        rl*value*function*1 1.000000  44.0625				19.999961853027344
//        rl*value*function*2 1.000000  10.0				same
//        rl*value*function*3 1.000000  10.0				same
//        rl*value*function*4 1.000000  15.0				same
//        rl*value*function*5 1.000000  10.0				same
//        rl*value*function*6 1.000000  20.0				15.0
//        rl*value*function*7 1.000000  10.0				same
//        rl*value*function*8 1.000000  20.0				18.75
//        rl*value*function*9 1.000000  10.0				same
        
        //	Check the correct value set
        double expectedValues[] = {19.999961853027344, 10.0, 10.0, 15.0, 10.0, 15.0, 10.0, 18.75, 10.0};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//			Value of hrl-discount:		off					on        
//        rl*value*function*1 2.000000  83.71875			37.99992752075195
//        rl*value*function*2 2.000000  19.5				same
//        rl*value*function*3 2.000000  19.0				same
//        rl*value*function*4 2.000000  28.75				same
//        rl*value*function*5 2.000000  19.0				same
//        rl*value*function*6 2.000000  38.5				28.75
//        rl*value*function*7 2.000000  19.0				same
//        rl*value*function*8 2.000000  38.125				35.6875
//        rl*value*function*9 2.000000  19.0				same
        
        //	Check the correct value set
        double expectedValues2[] = {37.99992752075195, 19.5, 19.0, 28.75, 19.0, 28.75, 19.0, 35.6875, 19.0};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues2));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//			Value of hrl-discount:		off					on        
//        rl*value*function*1 3.000000  119.409375			54.1998966217041
//        rl*value*function*2 3.000000  28.5				same
//        rl*value*function*3 3.000000  27.1				same
//        rl*value*function*4 3.000000  41.35				same
//        rl*value*function*5 3.000000  27.1				same
//        rl*value*function*6 3.000000  55.6				41.35
//        rl*value*function*7 3.000000  27.1				same
//        rl*value*function*8 3.000000  54.55				50.9875
//        rl*value*function*9 3.000000  27.1				same
        
        //	Check the correct value set
        double expectedValues3[] = {54.1998966217041, 28.5, 27.1, 41.35, 27.1, 41.35, 27.1, 50.9875, 27.1};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues3));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//			Value of hrl-discount:		off					on        
//        rl*value*function*1 4.000000  151.5309375			68.77986881256103
//        rl*value*function*2 4.000000  37.005				same
//        rl*value*function*3 4.000000  34.39				same
//        rl*value*function*4 4.000000  52.8925				same
//        rl*value*function*5 4.000000  34.39				same
//        rl*value*function*6 4.000000  71.395				52.8925
//        rl*value*function*7 4.000000  34.39				same
//        rl*value*function*8 4.000000  69.43375			64.808125
//        rl*value*function*9 4.000000  34.39				same

        //	Check the correct value set
        double expectedValues4[] = {68.77986881256103, 37.005, 34.39, 52.8925, 34.39, 52.8925, 34.39, 64.808125, 34.39};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues4));

    }
    @Test
    public void testRLUnitHrlDiscountOff() throws Exception
    {
    	//	Set hrl-discount off
        agent.getProperties()
        		.set(ReinforcementLearningParams.HRL_DISCOUNT,
        				ReinforcementLearningParams.HrlDiscount.off);

        runTest("testRLUnit", 25);
//			Value of hrl-discount:		off					on        
//        rl*value*function*1 1.000000  44.0625				19.999961853027344
//        rl*value*function*2 1.000000  10.0				same
//        rl*value*function*3 1.000000  10.0				same
//        rl*value*function*4 1.000000  15.0				same
//        rl*value*function*5 1.000000  10.0				same
//        rl*value*function*6 1.000000  20.0				15.0
//        rl*value*function*7 1.000000  10.0				same
//        rl*value*function*8 1.000000  20.0				18.75
//        rl*value*function*9 1.000000  10.0				same
        
        //	Check the correct value set
        double expectedValues[] = {44.0625, 10.0, 10.0, 15.0, 10.0, 20.0, 10.0, 20.0, 10.0};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//			Value of hrl-discount:		off					on        
//        rl*value*function*1 2.000000  83.71875			37.99992752075195
//        rl*value*function*2 2.000000  19.5				same
//        rl*value*function*3 2.000000  19.0				same
//        rl*value*function*4 2.000000  28.75				same
//        rl*value*function*5 2.000000  19.0				same
//        rl*value*function*6 2.000000  38.5				28.75
//        rl*value*function*7 2.000000  19.0				same
//        rl*value*function*8 2.000000  38.125				35.6875
//        rl*value*function*9 2.000000  19.0				same
        
        //	Check the correct value set
        double expectedValues2[] = {83.71875, 19.5, 19.0, 28.75, 19.0, 38.5, 19.0, 38.125, 19.0};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues2));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//			Value of hrl-discount:		off					on        
//        rl*value*function*1 3.000000  119.409375			54.1998966217041
//        rl*value*function*2 3.000000  28.5				same
//        rl*value*function*3 3.000000  27.1				same
//        rl*value*function*4 3.000000  41.35				same
//        rl*value*function*5 3.000000  27.1				same
//        rl*value*function*6 3.000000  55.6				41.35
//        rl*value*function*7 3.000000  27.1				same
//        rl*value*function*8 3.000000  54.55				50.9875
//        rl*value*function*9 3.000000  27.1				same
        
        //	Check the correct value set
        double expectedValues3[] = {119.409375, 28.5, 27.1, 41.35, 27.1, 55.6, 27.1, 54.55, 27.1};
        assertTrue("Actual RL values don't match expected values", checkExpectedValues("rl*value*function*", expectedValues3));
        
        agent.initialize();
        runTestExecute("testRLUnit", 25);
        
//			Value of hrl-discount:		off					on        
//        rl*value*function*1 4.000000  151.5309375			68.77986881256103
//        rl*value*function*2 4.000000  37.005				same
//        rl*value*function*3 4.000000  34.39				same
//        rl*value*function*4 4.000000  52.8925				same
//        rl*value*function*5 4.000000  34.39				same
//        rl*value*function*6 4.000000  71.395				52.8925
//        rl*value*function*7 4.000000  34.39				same
//        rl*value*function*8 4.000000  69.43375			64.808125
//        rl*value*function*9 4.000000  34.39				same

        //	Check the correct value set
        double expectedValues4[] = {151.5309375, 37.005, 34.39, 52.8925, 34.39, 71.395, 34.39, 69.43375, 34.39};
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
            if(p.rlRuleInfo != null && p.getName().startsWith(rulePrefix))
            {
                int startOfIndex = p.getName().lastIndexOf('*') + 1;
                int index = Integer.valueOf(p.getName().substring(startOfIndex)) - 1;
                double value = Double.valueOf(p.getFirstAction().asMakeAction().referent.toString());
                
                if(Math.abs(expectedValues[index] - value) > tolerance)
                {
                    result = false;
                    break;
                }
            }
        }
        
        return result;
    }
    
    
    //	Test that the chunk-stop parameter actual works	PL 8/21/2013
    private static final String chunkStopTestSoar1 = 
    		"elaborate*value*1\n"
    				+"	(state <s> ^operator <o> +)\n"
    		+"-->\n"
    				+"	(<s> ^operator <o> = 1)\n";

    private static final String chunkStopTestSoar2 = 
			"elaborate*value*2\n"
					+"	(state <s> ^operator <o> +)\n"
			+"-->\n"
					+"	(<s> ^operator <o> = 2)\n";
    
    @Test
    public void testChunkStopOn() throws Exception
    {
    	//	Set chunk-stop on
        agent.getProperties()
        		.set(ReinforcementLearningParams.CHUNK_STOP,
        				ReinforcementLearningParams.ChunkStop.on);

        //	Source the soar test code
        agent.getProductions().loadProduction(chunkStopTestSoar1);
        agent.getProductions().loadProduction(chunkStopTestSoar2);
    	
        //	See that there is only one production
        assertEquals(1, agent.getProductions().getProductionCount());
    }
    
    @Test
    public void testChunkStopOff() throws Exception
    {
    	//	Set chunk-stop off
        agent.getProperties()
        		.set(ReinforcementLearningParams.CHUNK_STOP,
        				ReinforcementLearningParams.ChunkStop.off);

        //	Source the soar test code
        agent.getProductions().loadProduction(chunkStopTestSoar1);
        agent.getProductions().loadProduction(chunkStopTestSoar2);
    	
        //	See that there are two productions
        assertEquals(2, agent.getProductions().getProductionCount());
    }

}
