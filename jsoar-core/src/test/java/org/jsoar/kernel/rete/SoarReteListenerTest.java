/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
 */
package org.jsoar.kernel.rete;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.symbols.GoalIdentifierInfo;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class SoarReteListenerTest {
  private Agent agent;

  @Before
  public void setUp() throws Exception {
    agent = new Agent("SoarReteListenerTest");
  }

  @After
  public void tearDown() throws Exception {
    agent.dispose();
    agent = null;
  }

  @Test
  public void testInterruptsTheAgentWhenTheInterruptFlagIsSet() throws Exception {
    agent
        .getProductions()
        .loadProduction(
            "propose*init\n"
                + "(state <s> ^superstate nil\n"
                + "          -^name)\n"
                + "-->\n"
                + "(<s> ^operator.name init)\n"
                + "");

    agent
        .getProductions()
        .loadProduction(
            "apply*init\n"
                + ":interrupt\n"
                + "(state <s> ^operator.name init)\n"
                + "-->\n"
                + "(<s> ^name done)\n"
                + "");

    agent.runFor(500, RunType.DECISIONS);

    // The rule should not have actually fired, just matched
    assertTrue(agent.getReasonForStop().contains("apply*init"));
    final Production p = agent.getProductions().getProduction("apply*init");
    assertEquals(0L, p.getFiringCount());
  }

  @Test
  public void testInterruptsTheAgentWhenBreakpointIsEnabled() throws Exception {
    agent
        .getProductions()
        .loadProduction(
            "propose*init\n"
                + "(state <s> ^superstate nil\n"
                + "          -^name)\n"
                + "-->\n"
                + "(<s> ^operator.name init)\n"
                + "");

    agent
        .getProductions()
        .loadProduction(
            "apply*init\n"
                + "(state <s> ^operator.name init)\n"
                + "-->\n"
                + "(<s> ^name done)\n"
                + "");
    final Production p = agent.getProductions().getProduction("apply*init");
    p.setBreakpointEnabled(true);

    agent.runFor(500, RunType.DECISIONS);

    // The rule should not have actually fired, just matched
    assertTrue(agent.getReasonForStop().contains("apply*init"));
    assertEquals(0L, p.getFiringCount());
  }

  @Test
  public void testAssertionsOrRetractionsReadyWithNilGoalRetractions() {
    // Given a soar rete listener
    SoarReteListener listener = new SoarReteListener(mock(Agent.class), mock(Rete.class));
    // And there are nil goal retractions
    listener.nil_goal_retractions = mock(ListHead.class);
    when(listener.nil_goal_retractions.isEmpty()).thenReturn(false);

    // When determining whether we are in a quiescence
    boolean quiescence = listener.any_assertions_or_retractions_ready();

    // Then result should be true
    assertTrue(quiescence);
  }

  @Test
  public void testAssertionsOrRetractionsReadyWithGoalWithOAssertions() {
    // Given a soar rete listener
    Agent agent = mock(Agent.class);
    when(agent.getDecider()).thenReturn(mock(Decider.class));
    SoarReteListener listener = new SoarReteListener(agent, mock(Rete.class));
    listener.initialize();
    // And there are no nil goal retractions
    listener.nil_goal_retractions = mock(ListHead.class);
    when(listener.nil_goal_retractions.isEmpty()).thenReturn(true);
    // And goal with o assertions
    IdentifierImpl id = mock(IdentifierImpl.class);
    agent.getDecider().bottom_goal = id;
    id.goalInfo = new GoalIdentifierInfo(id);
    id.goalInfo.ms_o_assertions.first = mock(ListItem.class);

    // When determining whether there are assertions or retraction ready
    boolean assertionsOrRetractionsReady = listener.any_assertions_or_retractions_ready();

    // Then result is true
    assertTrue(assertionsOrRetractionsReady);
  }

  @Test
  public void testAssertionsOrRetractionsReadyWithGoalWithIAssertions() {
    // Given a soar rete listener
    Agent agent = mock(Agent.class);
    when(agent.getDecider()).thenReturn(mock(Decider.class));
    SoarReteListener listener = new SoarReteListener(agent, mock(Rete.class));
    listener.initialize();
    // And there are no nil goal retractions
    listener.nil_goal_retractions = mock(ListHead.class);
    when(listener.nil_goal_retractions.isEmpty()).thenReturn(true);
    // And goal with i assertions
    IdentifierImpl id = mock(IdentifierImpl.class);
    agent.getDecider().bottom_goal = id;
    id.goalInfo = new GoalIdentifierInfo(id);
    id.goalInfo.ms_i_assertions.first = mock(ListItem.class);

    // When determining whether there are assertions or retraction ready
    boolean assertionsOrRetractionsReady = listener.any_assertions_or_retractions_ready();

    // Then result is true
    assertTrue(assertionsOrRetractionsReady);
  }

  @Test
  public void testAssertionsOrRetractionsReadyWithGoalWithRetractions() {
    // Given a soar rete listener
    Agent agent = mock(Agent.class);
    when(agent.getDecider()).thenReturn(mock(Decider.class));
    SoarReteListener listener = new SoarReteListener(agent, mock(Rete.class));
    listener.initialize();
    // And there are no nil goal retractions
    listener.nil_goal_retractions = mock(ListHead.class);
    when(listener.nil_goal_retractions.isEmpty()).thenReturn(true);
    // And goal with retractions
    IdentifierImpl id = mock(IdentifierImpl.class);
    agent.getDecider().bottom_goal = id;
    id.goalInfo = new GoalIdentifierInfo(id);
    id.goalInfo.ms_retractions.first = mock(ListItem.class);

    // When determining whether there are assertions or retraction ready
    boolean assertionsOrRetractionsReady = listener.any_assertions_or_retractions_ready();

    // Then result is true
    assertTrue(assertionsOrRetractionsReady);
  }

  @Test
  public void testAssertionsOrRetractionsReadyWithNoNillGoalRetractionsAndGoals() {
    // Given a soar rete listener
    Agent agent = mock(Agent.class);
    when(agent.getDecider()).thenReturn(mock(Decider.class));
    SoarReteListener listener = new SoarReteListener(agent, mock(Rete.class));
    listener.initialize();
    // And there are no nil goal retractions
    listener.nil_goal_retractions = mock(ListHead.class);
    when(listener.nil_goal_retractions.isEmpty()).thenReturn(true);
    // And no goals
    agent.getDecider().bottom_goal = null;

    // When determining whether there are assertions or retraction ready
    boolean assertionsOrRetractionsReady = listener.any_assertions_or_retractions_ready();

    // Then result is false
    assertFalse(assertionsOrRetractionsReady);
  }
}
