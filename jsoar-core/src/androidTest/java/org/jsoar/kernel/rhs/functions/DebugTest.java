/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel.rhs.functions;



import android.test.AndroidTestCase;

import org.jsoar.kernel.AbstractDebuggerProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class DebugTest extends AndroidTestCase
{

    private Agent agent;
    /**
     * @throws java.lang.Exception
     */
    @Override
    public void setUp() throws Exception
    {
        this.agent = new Agent(getContext());
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void tearDown() throws Exception
    {
    }

    public void testDebugCallsOpenDebugger() throws Exception
    {
        final ByRef<Boolean> called = ByRef.create(false);
        agent.setDebuggerProvider(new AbstractDebuggerProvider() {

            @Override
            public void openDebugger(Agent agent)
            {
                assertSame(DebugTest.this.agent, agent);
                called.value = true;
            }

            @Override
            public void openDebuggerAndWait(Agent agent) throws SoarException,
                    InterruptedException
            {
                throw new UnsupportedOperationException("openDebuggerAndWait");
            }
            
        });
        agent.getProductions().loadProduction("testDebugCallsOpenDebugger (state <s> ^superstate nil) --> (debug)");
        agent.runFor(1, RunType.DECISIONS);
        assertTrue(called.value.booleanValue());
    }
}
