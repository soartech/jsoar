/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *  
 * Created on Sep 2, 2010
 *  
 *  Updated by PL, 28 August 2013
 *
 *	This test runs some simple Soar code that is found in
 *	/org/jsoar/kernel/RLTests_testRLUnit.soar and then
 *	checks the numerical results.
 *
 *  Two possible sets of numbers are checked, depending
 *  on whether the hrl-discount parameter is on or off.
 *  You can set this parameter in the Soar file.
 *  
 *  This test is somewhat complementary to the one in
 *  org.jsoar.kernel.RLTests.java, which uses the same .soar file.
 */
package org.jsoar.kernel.learning.rl;


import static org.junit.Assert.*;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.RunType;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommands;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReinforcementLearningTest
{
    private Agent agent;
    private ReinforcementLearning rl;
    
    @Before
    public void setUp()
    {
        this.agent = new Agent();
        this.rl = Adaptables.adapt(this.agent, ReinforcementLearning.class);
        assertNotNull(rl);
    }
    
    @After
    public void tearDown()
    {
        this.agent.dispose();
        this.agent = null;
    }
    
    @Test
    public void testRunRLUnitTestSuite() throws Exception
    {
        // See also RLTests.
        
    	//	Source the soar test code
        final URL code = getClass().getResource("/org/jsoar/kernel/RLTests_testRLUnit.soar");
        SoarCommands.source(agent.getInterpreter(), code);

        //	Figure out what hrl-discount is/should be
        //	This must be done after the .soar file has been loaded
        boolean hrl_discount = agent.getProperties()
        		.get(ReinforcementLearningParams.HRL_DISCOUNT) == ReinforcementLearningParams.HrlDiscount.on;
        
        final Map<String, List<Double>> expectedValues = new HashMap<String, List<Double>>();
        if (hrl_discount) {
        	//	Check the values for hrl-discount = on
            expectedValues.put("rl*value*function*2", Arrays.asList(10., 19.5, 28.5, 37.005, 45.024));
            expectedValues.put("rl*value*function*3", Arrays.asList(10., 19., 27.1, 34.39, 40.951));
            expectedValues.put("rl*value*function*4", Arrays.asList(15., 28.75, 41.35, 52.8925, 63.463));
            expectedValues.put("rl*value*function*5", Arrays.asList(10., 19., 27.1, 34.39, 40.951));
            expectedValues.put("rl*value*function*6", Arrays.asList(15., 28.75, 41.35, 52.8925, 63.463));
            expectedValues.put("rl*value*function*7", Arrays.asList(10., 19., 27.1, 34.39, 40.951));
            expectedValues.put("rl*value*function*8", Arrays.asList(18.75, 35.6875, 50.9875, 64.808125, 77.29225));
            expectedValues.put("rl*value*function*9", Arrays.asList(10., 19., 27.1, 34.39, 40.951));
        } else {
        	//	Check the values for hrl-discount = off
            expectedValues.put("rl*value*function*2", Arrays.asList(10., 19.5,	28.5,	37.005,	45.024));
            expectedValues.put("rl*value*function*3", Arrays.asList(10., 19.,	27.1,	34.39,	40.951));
            expectedValues.put("rl*value*function*4", Arrays.asList(15., 28.75,	41.35,	52.8925,63.463));
            expectedValues.put("rl*value*function*5", Arrays.asList(10., 19.,	27.1,	34.39,	40.951));
            expectedValues.put("rl*value*function*6", Arrays.asList(20., 38.5,	55.6,	71.395,	85.975));
            expectedValues.put("rl*value*function*7", Arrays.asList(10., 19.,	27.1,	34.39,	40.951));
            expectedValues.put("rl*value*function*8", Arrays.asList(20., 38.125,54.55,	69.43375,82.92025));
            expectedValues.put("rl*value*function*9", Arrays.asList(10., 19.,	27.1,	34.39,	40.951));
        }

        //	Run a bunch of tests
        final int NUM_RUNS = 5;
        for(int run = 0; run < NUM_RUNS; run++)
        {
            agent.runFor(0, RunType.FOREVER);
            for(Map.Entry<String, List<Double>> e : expectedValues.entrySet())
            {
                final String ruleName = e.getKey();
                final double expectedValue = e.getValue().get(run);
                
                final Production rule = agent.getProductions().getProduction(ruleName);
                assertNotNull(rule);
                Double actualValue = rule.getFirstAction().asMakeAction().referent
                						.asSymbolValue().getSym().asDouble().getValue();
//                System.out.println(String.format("Run %d: expected %f, got %f.",
//                					run, expectedValue, actualValue));
                assertEquals(expectedValue, 
                			 actualValue, 
                             0.00000001);
            }
            agent.initialize();
        }
    }
}
