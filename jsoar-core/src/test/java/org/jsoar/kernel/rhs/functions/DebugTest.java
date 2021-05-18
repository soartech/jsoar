/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.SoarException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class DebugTest {

  private Agent agent;

  @Before
  public void setUp() throws Exception {
    this.agent = new Agent();
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testCreateDebugProduction() {
    // When creating new debug production
    Debug production = new Debug(mock(Agent.class));

    // Then name of production is get-url
    assertEquals("debug", production.getName());
    // And production requires 0 arguments
    assertEquals(0, production.getMinArguments());
    assertEquals(0, production.getMaxArguments());
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfOpeningDebuggerFails()
      throws SoarException, RhsFunctionException {
    // Given Agent
    Agent agent = mock(Agent.class);
    // And a debug production instance
    Debug debug = new Debug(agent);

    // When executing debug production
    // and opening debugger fails
    DebuggerProvider debuggerProvider = mock(DebuggerProvider.class);
    when(agent.getDebuggerProvider()).thenReturn(debuggerProvider);
    doThrow(SoarException.class).when(debuggerProvider).openDebugger(agent);
    debug.execute(mock(RhsFunctionContext.class), Collections.emptyList());
  }

  @Test
  public void testExecute() throws RhsFunctionException, SoarException {
    // Given a Agent
    Agent agent = mock(Agent.class);
    // And a debugger provider
    DebuggerProvider debuggerProvider = mock(DebuggerProvider.class);
    when(agent.getDebuggerProvider()).thenReturn(debuggerProvider);
    // And a debug production
    Debug debug = new Debug(agent);

    // When executing debug production
    debug.execute(mock(RhsFunctionContext.class), Collections.emptyList());

    // Then debugger of specified provider is opened
    verify(debuggerProvider, times(1)).openDebugger(agent);
  }
}
