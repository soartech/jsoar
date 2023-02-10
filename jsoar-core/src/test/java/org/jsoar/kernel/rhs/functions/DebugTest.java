/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoar.kernel.AbstractDebuggerProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.ByRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class DebugTest
{
    
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        this.agent = new Agent();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception
    {
    }
    
    @Test
    public void testDebugCallsOpenDebugger() throws Exception
    {
        final ByRef<Boolean> called = ByRef.create(false);
        agent.setDebuggerProvider(new AbstractDebuggerProvider()
        {
            
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
            
            @Override
            public void closeDebugger(Agent agent)
            {
                throw new UnsupportedOperationException("closeDebugger");
            }
            
            @Override
            public Object getDebugger(Agent agent)
            {
                throw new UnsupportedOperationException("getDebugger");
            }
            
        });
        agent.getProductions().loadProduction("testDebugCallsOpenDebugger (state <s> ^superstate nil) --> (debug)");
        agent.runFor(1, RunType.DECISIONS);
        assertTrue(called.value.booleanValue());
    }
}
