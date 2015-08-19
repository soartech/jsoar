/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.commands;


import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.commands.DefaultSoarCommandContext;

public class SaveBacktracesCommandTest extends AndroidTestCase
{
    private Agent agent;
    private SaveBacktracesCommand command;
    
    @Override
    public void setUp() throws Exception
    {
        this.agent = new Agent(getContext());
        this.command = new SaveBacktracesCommand(agent);
    }

    @Override
    public void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
    public void testEnableShouldSetExplainPropertyToTrue() throws Exception
    {
        agent.getProperties().set(SoarProperties.EXPLAIN, false);
        command.execute(DefaultSoarCommandContext.empty(), new String[] {"save-backtraces", "-e" });
        assertTrue(agent.getProperties().get(SoarProperties.EXPLAIN));
    }
    
    public void testDisableShouldSetExplainPropertyToFalse() throws Exception
    {
        agent.getProperties().set(SoarProperties.EXPLAIN, true);
        command.execute(DefaultSoarCommandContext.empty(), new String[] {"save-backtraces", "--off" });
        assertFalse(agent.getProperties().get(SoarProperties.EXPLAIN));
    }

}
