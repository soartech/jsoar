/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 30, 2010
 */
package org.jsoar.kernel;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class AgentTest {
  private Agent agent;

  @Before
  public void setUp() throws Exception {
    agent = new Agent();
  }

  @After
  public void tearDown() throws Exception {
    agent.dispose();
  }

  @Test
  public void testDefaultStopPhaseIsApply() {
    assertEquals(Phase.APPLY, agent.getStopPhase());
  }

  @Test
  public void testSetStopPhaseSetsTheStopPhaseProperty() {
    agent.setStopPhase(Phase.DECISION);
    assertEquals(Phase.DECISION, agent.getStopPhase());
    assertEquals(Phase.DECISION, agent.getProperties().get(SoarProperties.STOP_PHASE));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxElaborationsThrowsExceptionIfSmallerThanZero() {
    agent.getProperties().set(SoarProperties.MAX_ELABORATIONS, -1);
  }

  @Test
  public void testGetGoalStack() {
    agent.runFor(3, RunType.DECISIONS);
    // We start with S1. Running three steps, gives three new states, S2, S3, S4
    final List<Goal> gs = agent.getGoalStack();
    assertNotNull(gs);
    assertEquals(4, gs.size());
    final SymbolFactory syms = agent.getSymbols();
    assertEquals(
        Arrays.asList(
            syms.findIdentifier('S', 1),
            syms.findIdentifier('S', 3),
            syms.findIdentifier('S', 5),
            syms.findIdentifier('S', 7)),
        Arrays.asList(
            gs.get(0).getIdentifier(),
            gs.get(1).getIdentifier(),
            gs.get(2).getIdentifier(),
            gs.get(3).getIdentifier()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetDebuggerProviderThrownAnExceptionIfNull() {
    agent.setDebuggerProvider(null);
  }

  @Test
  public void testPrintStackTrace() throws IOException {

    Writer outputWriter = mock(Writer.class);
    agent.getPrinter().addPersistentWriter(outputWriter);
    agent.printStackTrace(true, true);

    verify(outputWriter, atLeast(1)).write(any(char[].class), anyInt(), anyInt());
  }
}
