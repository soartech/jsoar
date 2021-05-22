/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.symbols;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.List;
import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.GoalDependencySetImpl;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class IdentifierImplTest extends JSoarTest {
  private Agent agent;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    this.agent = new Agent();
  }

  @Test
  public void testIsLongTermIdentifier() {
    IdentifierImpl identifier;

    // Given Identifier
    identifier = new IdentifierImpl(mock(SymbolFactoryImpl.class), 0, 'A', 6);
    // And identifier has semantic memory id
    identifier.semanticMemoryId = 123;

    // Then identifier is a long term identifier
    assertTrue(identifier.isLongTermIdentifier());

    // Given Identifier
    identifier = new IdentifierImpl(mock(SymbolFactoryImpl.class), 0, 'A', 6);
    // And identifier has NO semantic memory id
    identifier.semanticMemoryId = 0;

    // Then identifier is a short term identifier
    assertFalse(identifier.isLongTermIdentifier());
  }

  @Test
  public void testToStringLongTermIdentifier() {
    // Given Identifier with name letter 'A' and number 6
    IdentifierImpl identifier = new IdentifierImpl(mock(SymbolFactoryImpl.class), 0, 'A', 6);
    // And Identifier is Long Term Identifier
    identifier.semanticMemoryId = 123;

    // When getting textual representation of identifier
    String text = identifier.toString();

    // Then identifier starts with @
    // Followed by name letter and name number
    assertEquals("@A6", text);
  }

  @Test
  public void testToStringShortTermIdentifier() {
    // Given Identifier with name letter 'A' and number 6
    IdentifierImpl identifier = new IdentifierImpl(mock(SymbolFactoryImpl.class), 0, 'A', 6);
    // And Identifier is Short Term Identifier
    identifier.semanticMemoryId = 0;

    // When getting textual representation of identifier
    String text = identifier.toString();

    // Then identifier starts with @
    // Followed by name letter and name number
    assertEquals("A6", text);
  }

  /** Test method for {@link org.jsoar.kernel.symbols.IdentifierImpl#getWmes()}. */
  @Test
  public void testGetWmes() throws Exception {
    // Load a production that creates some WMEs off of S1, then
    // test iterating over them.
    agent
        .getProductions()
        .loadProduction(
            "testGetWmes "
                + "(state <s> ^superstate nil)"
                + "-->"
                + "(<s> ^test <w>)"
                + "(<w> ^a 1 ^b 2 ^c 3 ^d 4)");

    agent.runFor(1, RunType.DECISIONS);

    final Identifier id = agent.getSymbols().findIdentifier('S', 1);
    assertNotNull(id);

    // Find the test wme
    final MatcherBuilder m = Wmes.matcher(agent);
    final Wme test = m.attr("test").find(id);
    assertNotNull(test);

    // Now verify that all the sub-wmes are there.
    List<Wme> kids = new ArrayList<>();
    final Identifier testId = test.getValue().asIdentifier();
    Iterators.addAll(kids, testId.getWmes());
    assertEquals(4, kids.size());
    assertNotNull(m.reset().attr("a").value(1).find(kids));
    assertNotNull(m.reset().attr("b").value(2).find(kids));
    assertNotNull(m.reset().attr("c").value(3).find(kids));
    assertNotNull(m.reset().attr("d").value(4).find(kids));
  }

  @Test
  public void testIsAdaptableToGoalDependencySet() {
    final IdentifierImpl id = syms.createIdentifier('S');
    id.goalInfo = new GoalIdentifierInfo(id);
    assertNull(Adaptables.adapt(id, GoalDependencySet.class));
    id.goalInfo.gds = new GoalDependencySetImpl(id);
    assertSame(id.goalInfo.gds, Adaptables.adapt(id, GoalDependencySet.class));
  }

  @Test
  public void testFormatsLongTermIdentifiersCorrectly() {
    final IdentifierImpl id = syms.createIdentifier('S');
    id.semanticMemoryId = 99;
    assertEquals("@S" + id.getNameNumber(), String.format("%s", id));
  }

  @Test
  public void testStateIsAdaptableToGoal() {
    final Identifier state = agent.getSymbols().findIdentifier('S', 1);
    assertNotNull(state);

    final Goal goal = Adaptables.adapt(state, Goal.class);
    assertNotNull(goal);
  }

  @Test
  public void testNonStatesAreNotAdaptableToGoal() {
    final Identifier id = agent.getSymbols().createIdentifier('Z');
    assertNotNull(id);
    assertFalse(id.isGoal());

    final Goal goal = Adaptables.adapt(id, Goal.class);
    assertNull(goal);
  }

  @Test
  public void testAdaptToGoalAndGetSelectedOperatorName() throws Exception {
    agent
        .getProductions()
        .loadProduction(
            "propose (state <s> ^superstate nil) --> (<s> ^operator.name test-operator)");
    agent.runFor(1, RunType.DECISIONS);

    final Goal state = agent.getGoalStack().get(0);
    assertNotNull(state);

    // just verify adaptability here..
    final Goal goal = Adaptables.adapt(state.getIdentifier(), Goal.class);
    assertNotNull(goal);

    // test the operator name
    assertEquals("test-operator", goal.getOperatorName().asString().getValue());
  }

  @Test
  public void testAddSlotToIdentifierWithSlots() {
    // Given a identifier
    final IdentifierImpl id = syms.createIdentifier('S');
    // With a existing Slot
    Slot existingSlot = mock(Slot.class);
    id.slots = existingSlot;

    // When adding new slot to identifier
    Slot newSlot = mock(Slot.class);
    id.addSlot(newSlot);

    // Then slots of identifier points to new Slot
    assertEquals(newSlot, id.slots);
    // And new Slot if pointing to existing Slot in list
    assertEquals(existingSlot, newSlot.next);
    // And new Slot is at begin of list
    assertNull(newSlot.prev);
    // And existing Slot is pointing to new Slot
    assertEquals(newSlot, existingSlot.prev);
  }

  @Test
  public void testAddSlotToIdentifierWithoutSlots() {
    // Given a identifier
    final IdentifierImpl id = syms.createIdentifier('S');

    // When adding new slot to identifier
    Slot newSlot = mock(Slot.class);
    id.addSlot(newSlot);

    // Then slots of identifier points to new Slot
    assertEquals(newSlot, id.slots);
    // And new Slot is at end of list
    assertNull(newSlot.next);
    // And new Slot is at begin of list
    assertNull(newSlot.prev);
  }

  @Test
  public void testAddInvalidSlotToIdentifier() {
    // Given a identifier
    final IdentifierImpl id = syms.createIdentifier('S');

    // When adding null as Slot to identifier
    // Then Illegal Argument Exception is thrown
    assertThrows(IllegalArgumentException.class, () -> id.addSlot(null));
  }
}
