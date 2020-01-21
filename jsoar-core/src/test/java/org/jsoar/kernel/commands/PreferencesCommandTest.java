/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 21, 2009
 */
package org.jsoar.kernel.commands;


import static org.junit.Assert.assertNotNull;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.RunType;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class PreferencesCommandTest
{
    private Agent agent;
    
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.getTrace().disableAll();
    }

    @After
    public void tearDown() throws Exception
    {
        if(this.agent != null)
        {
            this.agent.dispose();
            this.agent = null;
        }
    }
    
    @Test
    public void testThatRequiredAgentInternalsArePresent()
    {
        // PreferencesCommand relies on Decider and PredefinedSymbols
        assertNotNull("Decider not found in Agent", Adaptables.adapt(agent, Decider.class));
        assertNotNull("PredefinedSymbols not found in Agent", Adaptables.adapt(agent, PredefinedSymbols.class));
    }

    @Test
    public void testThatAttributeParametersAreHandledCorrectly() throws Exception
    {
        agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (<s> ^foo 10)");
        agent.runFor(1, RunType.DECISIONS);
        agent.getInterpreter().eval("preferences S1 ^foo");
        
        // No exception should be thrown
    }
    @Test
    public void testThatAttributeParametersWithHyphensAreHandledCorrectly() throws Exception
    {
        agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (<s> ^foo-bar 10)");
        agent.runFor(1, RunType.DECISIONS);
        agent.getInterpreter().eval("preferences S1 ^foo-bar");
        
        // No exception should be thrown
    }
}
