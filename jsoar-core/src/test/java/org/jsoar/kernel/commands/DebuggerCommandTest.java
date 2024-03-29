/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 9, 2010
 */
package org.jsoar.kernel.commands;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoar.kernel.AbstractDebuggerProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.jupiter.api.Test;

class DebuggerCommandTest
{
    @Test
    void testDebuggerCommandCallsOpenDebuggerOnAgent() throws Exception
    {
        final Agent agent = new Agent("testDebuggerCommandCallsOpenDebuggerOnAgent");
        final AtomicBoolean called = new AtomicBoolean(false);
        final DebuggerProvider provider = new AbstractDebuggerProvider()
        {
            
            @Override
            public void openDebuggerAndWait(Agent agent) throws SoarException,
                    InterruptedException
            {
                throw new UnsupportedOperationException("openDebuggerAndWait not supported");
            }
            
            @Override
            public void openDebugger(Agent a) throws SoarException
            {
                assertSame(agent, a);
                called.set(true);
            }
            
            @Override
            public void closeDebugger(Agent agent)
            {
                throw new UnsupportedOperationException("closeDebugger not supported");
            }
            
            @Override
            public Object getDebugger(Agent agent)
            {
                throw new UnsupportedOperationException("closeDebugger not supported");
            }
        };
        agent.setDebuggerProvider(provider);
        final DebuggerCommand command = new DebuggerCommand(agent);
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "debugger" });
        assertTrue(called.get());
    }
}
