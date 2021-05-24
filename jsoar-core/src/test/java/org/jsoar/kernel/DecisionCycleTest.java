/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 16, 2008
 */
package org.jsoar.kernel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class DecisionCycleTest {
  private Agent agent;
  private DecisionCycle decisionCycle;

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {
    this.agent = new Agent();
    this.agent.getTrace().enableAll();

    this.decisionCycle = Adaptables.adapt(this.agent, DecisionCycle.class);
  }

  /** @throws java.lang.Exception */
  @After
  public void tearDown() throws Exception {
    this.agent.getPrinter().flush();
  }

  @Test
  public void testDoOneTopLevelPhaseWithEmptyAgent() throws Exception {
    final Decider decider = Adaptables.adapt(agent, Decider.class);
    for (int i = 1; i < 10; ++i) {
      assertEquals(Phase.INPUT, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);
      assertEquals(Phase.PROPOSE, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);
      assertEquals(Phase.DECISION, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);
      assertEquals(Phase.APPLY, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);
      assertEquals(Phase.OUTPUT, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);

      // Verify that new states are being generated
      assertEquals("S" + (1 + 2 * i), decider.bottomGoal().toString());
    }
  }

  @Test
  public void testDoOneTopLevelPhaseWithSimpleProduction() throws Exception {
    agent.getProductions().loadProduction("test1 (state <s> ^superstate nil) --> (<s> ^foo 1)");
    agent
        .getProductions()
        .loadProduction(
            "test2 (state <s> ^superstate nil ^foo 1) --> (write (crlf) |test2 matched!|)");

    assertTrue(agent.getProductions().getProduction("test2").instantiations == null);

    assertEquals(Phase.INPUT, this.decisionCycle.current_phase.get());
    this.decisionCycle.runFor(1, RunType.PHASES);
    assertEquals(Phase.PROPOSE, this.decisionCycle.current_phase.get());
    this.decisionCycle.runFor(1, RunType.PHASES);
    assertEquals(Phase.DECISION, this.decisionCycle.current_phase.get());
    this.decisionCycle.runFor(1, RunType.PHASES);
    assertEquals(Phase.APPLY, this.decisionCycle.current_phase.get());
    this.decisionCycle.runFor(1, RunType.PHASES);
    assertEquals(Phase.OUTPUT, this.decisionCycle.current_phase.get());
    this.decisionCycle.runFor(1, RunType.PHASES);

    // verify that (S1 foo 1) is being added to the rete by checking that test2 fired
    assertFalse(agent.getProductions().getProduction("test2").instantiations == null);

    // Verify that new states are being generates
    final Decider decider = Adaptables.adapt(agent, Decider.class);
    assertEquals("S3", decider.bottomGoal().toString());
  }

  @Test
  public void testWaitOperatorOnStateNoChange() throws Exception {
    // A production that just proposes a wait operator every cycle
    agent
        .getProductions()
        .loadProduction(
            "top-state*propose*wait\n"
                + "   (state <s> ^attribute state\n"
                + "              ^choices none\n"
                + "             -^operator.name wait)\n"
                + "-->\n"
                + "   (<s> ^operator <o> +)\n"
                + "   (<o> ^name wait)");

    for (int i = 1; i < 10; ++i) {
      assertEquals(Phase.INPUT, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);
      assertEquals(Phase.PROPOSE, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);
      assertEquals(Phase.DECISION, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);
      assertEquals(Phase.APPLY, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);
      assertEquals(Phase.OUTPUT, this.decisionCycle.current_phase.get());
      this.decisionCycle.runFor(1, RunType.PHASES);

      // Verify that one state-no-change occurs, producing S3, but no further
      // states are generated. Also verify that the current operator.
      final Decider decider = Adaptables.adapt(agent, Decider.class);
      assertEquals("S3", decider.bottomGoal().toString());
      validateLastOperator(i);
    }
  }

  @Test
  public void testHaltRHS() throws Exception {
    agent
        .getProductions()
        .loadProduction("test1 (state <s> ^superstate nil) --> (<s> ^operator.name halt)");
    agent.getProductions().loadProduction("test2 (state <s> ^operator.name halt) --> (halt)");
    for (int i = 0; i < 3; i++) {
      if (i <= 3) {
        this.decisionCycle.runFor(1, RunType.PHASES);
        assertFalse(decisionCycle.isHalted());
        assertFalse(decisionCycle.isStopped());
      } else {
        this.decisionCycle.runFor(1, RunType.PHASES);
        assertTrue(decisionCycle.isHalted());
        assertTrue(decisionCycle.isStopped());
      }
    }
  }

  @Test
  public void testStopDecisionCycle() {
    // Given a active decision cycle
    DecisionCycle cycle = new DecisionCycle(mock(Agent.class));
    assertFalse(cycle.isStopped());

    // When stopping decision cycle
    cycle.stop();

    // Then decision cycle is stopped
    assertTrue(cycle.isStopped());
    // And reason for stop is 'Stopped by user.'
    assertEquals("Stopped by user.", cycle.getReasonForStop());
  }

  @Test
  public void testStopDecisionCycleWhichIsHalted() {
    final String reasonForHalting = "REASON";

    // Given a halted decision cycle
    DecisionCycle cycle = new DecisionCycle(mock(Agent.class));
    cycle.halt(reasonForHalting);

    // When stopping decision cycle
    cycle.stop();

    // Then decision cycle is stopped
    assertTrue(cycle.isStopped());
    // And reason for stop is unchanged
    assertEquals(reasonForHalting, cycle.getReasonForStop());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRunForThrowsExceptionIfRunTypePhasesAndNumberOfPhasesSmallerThanZero() {
    // Given a decision cycle
    DecisionCycle cycle = new DecisionCycle(mock(Agent.class));

    // When running for individual phases for less than zero cycles
    // Then IllegalArgumentException is thrown
    cycle.runFor(-1, RunType.PHASES);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRunForThrowsExceptionIfRunTypeElaborationsAndNumberOfPhasesSmallerThanZero() {
    // Given a decision cycle
    DecisionCycle cycle = new DecisionCycle(mock(Agent.class));

    // When running for individual phases for less than zero cycles
    // Then IllegalArgumentException is thrown
    cycle.runFor(-1, RunType.ELABORATIONS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRunForThrowsExceptionIfRunTypeDecisionsAndNumberOfPhasesSmallerThanZero() {
    // Given a decision cycle
    DecisionCycle cycle = new DecisionCycle(mock(Agent.class));

    // When running for individual phases for less than zero cycles
    // Then IllegalArgumentException is thrown
    cycle.runFor(-1, RunType.DECISIONS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void
      testRunForThrowsExceptionIfRunTypeModificationsOfOutputAndNumberOfPhasesSmallerThanZero() {
    // Given a decision cycle
    DecisionCycle cycle = new DecisionCycle(mock(Agent.class));

    // When running for individual phases for less than zero cycles
    // Then IllegalArgumentException is thrown
    cycle.runFor(-1, RunType.MODIFICATIONS_OF_OUTPUT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRunForThrowsExceptionIfRunTypeForeverNumberOfPhasesSmallerThanZero() {
    // Given a decision cycle
    DecisionCycle cycle = new DecisionCycle(mock(Agent.class));

    // When running for individual phases for less than zero cycles
    // Then IllegalArgumentException is thrown
    cycle.runFor(-1, RunType.FOREVER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRunForThrowsExceptionIfRunTypeIsNull() {
    // Given a decision cycle
    DecisionCycle cycle = new DecisionCycle(mock(Agent.class));

    // When running for individual phases for less than zero cycles
    // Then IllegalArgumentException is thrown
    cycle.runFor(1, null);
  }

  private void validateLastOperator(long number) {
    final SymbolFactoryImpl syms = Adaptables.adapt(agent, SymbolFactoryImpl.class);
    IdentifierImpl last = syms.findIdentifier('O', number);
    assertNotNull(last);
    assertTrue(last.isa_operator > 0);

    IdentifierImpl next = syms.findIdentifier('O', number + 1);
    assertNull(next);
  }
}
