/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SaveBacktracesCommandTest
{
    private Agent agent;
    private SaveBacktracesCommand command;
    
    @BeforeEach
    void setUp() throws Exception
    {
        this.agent = new Agent();
        this.command = new SaveBacktracesCommand(agent);
    }
    
    @AfterEach
    void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
    @Test
    void testEnableShouldSetExplainPropertyToTrue() throws Exception
    {
        agent.getProperties().set(SoarProperties.EXPLAIN, false);
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "save-backtraces", "-e" });
        assertTrue(agent.getProperties().get(SoarProperties.EXPLAIN));
    }
    
    @Test
    void testDisableShouldSetExplainPropertyToFalse() throws Exception
    {
        agent.getProperties().set(SoarProperties.EXPLAIN, true);
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "save-backtraces", "--off" });
        assertFalse(agent.getProperties().get(SoarProperties.EXPLAIN));
    }
    
}
