/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 21, 2009
 */
package org.jsoar.kernel.commands;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.RunType;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class PreferencesCommandTest
{
    private Agent agent;
    
    @BeforeEach
    void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.getTrace().disableAll();
    }
    
    @AfterEach
    void tearDown() throws Exception
    {
        if(this.agent != null)
        {
            this.agent.dispose();
            this.agent = null;
        }
    }
    
    @Test
    void testThatRequiredAgentInternalsArePresent()
    {
        // PreferencesCommand relies on Decider and PredefinedSymbols
        assertNotNull(Adaptables.adapt(agent, Decider.class), "Decider not found in Agent");
        assertNotNull(Adaptables.adapt(agent, PredefinedSymbols.class), "PredefinedSymbols not found in Agent");
    }
    
    @Test
    void testThatAttributeParametersAreHandledCorrectly() throws Exception
    {
        agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (<s> ^foo 10)");
        agent.runFor(1, RunType.DECISIONS);
        agent.getInterpreter().eval("preferences S1 ^foo");
        
        // No exception should be thrown
    }
    
    @Test
    void testThatAttributeParametersWithHyphensAreHandledCorrectly() throws Exception
    {
        agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (<s> ^foo-bar 10)");
        agent.runFor(1, RunType.DECISIONS);
        agent.getInterpreter().eval("preferences S1 ^foo-bar");
        
        // No exception should be thrown
    }
}
