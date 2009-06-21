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
import org.jsoar.util.adaptables.Adaptables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class PreferencesCommandTest
{

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }
    
    @Test
    public void testThatRequiredAgentInternalsArePresent()
    {
        // PreferencesCommand relies on Decider and PredefinedSymbols
        final Agent agent = new Agent();
        assertNotNull("Decider not found in Agent", Adaptables.adapt(agent, Decider.class));
        assertNotNull("PredefinedSymbols not found in Agent", Adaptables.adapt(agent, PredefinedSymbols.class));
    }

}
