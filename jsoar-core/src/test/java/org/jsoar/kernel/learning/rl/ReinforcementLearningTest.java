/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
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
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.exploration.Exploration.Policy;
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
        this.agent.initialize();
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
    public void testSetsExplorationModeToEpsilonGreedyWhenEnabled()
    {
        final Exploration explore = Adaptables.adapt(agent, Exploration.class);
        assertNotNull(explore);
        
        assertNotSame(Policy.USER_SELECT_E_GREEDY, explore.exploration_get_policy());
        
        agent.getProperties().set(ReinforcementLearning.LEARNING, true);
        assertSame(Policy.USER_SELECT_E_GREEDY, explore.exploration_get_policy());
    }
    
    @Test
    public void testRunRLUnitTestSuite() throws Exception
    {
        // See also RLTests.
        
        final int NUM_RUNS = 5;
        final Map<String, List<Double>> expectedValues = new HashMap<String, List<Double>>();
        expectedValues.put("rl*value*function*2", Arrays.asList(10., 19.5, 28.5, 37.005, 45.024));
        expectedValues.put("rl*value*function*3", Arrays.asList(10., 19., 27.1, 34.39, 40.951));
        expectedValues.put("rl*value*function*4", Arrays.asList(15., 28.75, 41.35, 52.8925, 63.463));
        expectedValues.put("rl*value*function*5", Arrays.asList(10., 19., 27.1, 34.39, 40.951));
        expectedValues.put("rl*value*function*6", Arrays.asList(15., 28.75, 41.35, 52.8925, 63.463));
        expectedValues.put("rl*value*function*7", Arrays.asList(10., 19., 27.1, 34.39, 40.951));
        expectedValues.put("rl*value*function*8", Arrays.asList(18.75, 35.6875, 50.9875, 64.808125, 77.29225));
        expectedValues.put("rl*value*function*9", Arrays.asList(10., 19., 27.1, 34.39, 40.951));
        
        final URL code = getClass().getResource("/org/jsoar/kernel/RLTests_testRLUnit.soar");
        SoarCommands.source(agent.getInterpreter(), code);
        for(int run = 0; run < NUM_RUNS; run++)
        {
            agent.runFor(0, RunType.FOREVER);
            for(Map.Entry<String, List<Double>> e : expectedValues.entrySet())
            {
                final String ruleName = e.getKey();
                final double expectedValue = e.getValue().get(run);
                
                final Production rule = agent.getProductions().getProduction(ruleName);
                assertNotNull(rule);
                assertEquals(expectedValue, 
                             rule.action_list.asMakeAction().referent.asSymbolValue().getSym().asDouble().getValue(), 
                             0.00000001);
            }
            agent.initialize();
        }
    }
}
