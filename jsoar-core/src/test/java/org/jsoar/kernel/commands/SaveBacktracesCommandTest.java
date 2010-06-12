/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.commands;


import static org.junit.Assert.*;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SaveBacktracesCommandTest
{
    private Agent agent;
    private SaveBacktracesCommand command;
    
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.command = new SaveBacktracesCommand(agent);
    }

    @After
    public void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
    @Test
    public void testEnableShouldSetExplainPropertyToTrue() throws Exception
    {
        agent.getProperties().set(SoarProperties.EXPLAIN, false);
        command.execute(new String[] {"save-backtraces", "-e" });
        assertTrue(agent.getProperties().get(SoarProperties.EXPLAIN));
    }
    
    @Test
    public void testDisableShouldSetExplainPropertyToTrue() throws Exception
    {
        agent.getProperties().set(SoarProperties.EXPLAIN, true);
        command.execute(new String[] {"save-backtraces", "--off" });
        assertFalse(agent.getProperties().get(SoarProperties.EXPLAIN));
    }

}
