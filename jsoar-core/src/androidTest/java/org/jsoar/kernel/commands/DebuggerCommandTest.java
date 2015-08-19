/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 9, 2010
 */
package org.jsoar.kernel.commands;


import android.test.AndroidTestCase;

import org.jsoar.kernel.AbstractDebuggerProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultSoarCommandContext;

import java.util.concurrent.atomic.AtomicBoolean;

public class DebuggerCommandTest extends AndroidTestCase
{
    public void testDebuggerCommandCallsOpenDebuggerOnAgent() throws Exception
    {
        final Agent agent = new Agent("testDebuggerCommandCallsOpenDebuggerOnAgent", getContext());
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
        };
        agent.setDebuggerProvider(provider);
        final DebuggerCommand command = new DebuggerCommand(agent);
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "debugger" });
        assertTrue(called.get());
    }
}
