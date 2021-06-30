/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.events.OutputEvent.OutputMode;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.StandaloneRhsFunctionHandler;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class InputOutputImplTest extends JSoarTest {
  private Agent agent;
  private SoarCommandInterpreter ifc;

  private static class MatchFunction extends StandaloneRhsFunctionHandler {
    boolean called = false;
    List<List<Symbol>> calls = new ArrayList<List<Symbol>>();

    public MatchFunction() {
      super("match");
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments)
        throws RhsFunctionException {
      called = true;
      calls.add(new ArrayList<Symbol>(arguments));
      return null;
    }
  }

  private MatchFunction match;

  private void sourceTestFile(String name) throws SoarException {
    ifc.source(getClass().getResource("/" + getClass().getName().replace('.', '/') + "_" + name));
  }

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {
    super.setUp();

    agent = new Agent(false);
    ifc = agent.getInterpreter();
    agent.getRhsFunctions().registerHandler(match = new MatchFunction());
    agent.initialize();

    // Since this is the InputOutput tests, these tests have to stop after output
    // (ie. before INPUT). I changed this to Phase.APPLY so this broke all the tests.
    // - ALT
    agent.setStopPhase(Phase.INPUT);
  }

  /** @throws java.lang.Exception */
  @After
  public void tearDown() throws Exception {
    agent.dispose();
  }

  @Test
  public void testBasicInput() throws Exception {
    final int listenerCallCount[] = {0};
    agent
        .getEvents()
        .addListener(
            InputEvent.class,
            new SoarEventListener() {

              @Override
              public void onEvent(SoarEvent event) {
                listenerCallCount[0]++;

                InputBuilder builder = InputBuilder.create(agent.getInputOutput());
                builder
                    .push("location")
                    .markId("L1")
                    .add("x", 3)
                    .add("y", 4)
                    .add("name", "hello")
                    .pop()
                    .push("location")
                    .markId("L2")
                    .add("x", 5)
                    .add("y", 6)
                    .add("name", "goodbye")
                    .link("a link", "L1")
                    .pop()
                    .add(99, "integer attribute")
                    .add(3.0, "double attribute")
                    .add("flag", Symbols.NEW_ID);
              }
            });

    sourceTestFile("testBasicInput.soar");
    agent.runFor(3, RunType.DECISIONS);
    assertEquals(3, listenerCallCount[0]);
    if (!match.called) {
      ifc.eval("matches testBasicInput");
    }
    assertTrue(match.called);
  }

  @Test
  public void testAddAndRemoveInputWme() throws Exception {
    final InputWme[] wme = {null};
    agent
        .getEvents()
        .addListener(
            InputEvent.class,
            new SoarEventListener() {

              @Override
              public void onEvent(SoarEvent event) {
                if (wme[0] == null) {
                  InputBuilder builder = InputBuilder.create(agent.getInputOutput());
                  builder.add("location", "ann arbor").markWme("wme");
                  wme[0] = builder.getWme("wme");
                } else {
                  wme[0].remove();
                }
              }
            });

    sourceTestFile("testAddAndRemoveInputWme.soar");

    // Next the "added" production will fire because the WME has been added
    agent.runFor(1, RunType.DECISIONS);
    assertEquals(1, match.calls.size());
    assertEquals("added", match.calls.get(0).get(0).toString());

    // Finally, "removed" fires again after the WME has been removed
    agent.runFor(1, RunType.DECISIONS);
    assertEquals(2, match.calls.size());
    assertEquals("removed", match.calls.get(1).get(0).toString());
  }

  @Test
  public void testBasicOutput() throws Exception {

    final List<Set<Wme>> outputs = new ArrayList<Set<Wme>>();

    agent
        .getEvents()
        .addListener(
            InputEvent.class,
            new SoarEventListener() {

              @Override
              public void onEvent(SoarEvent event) {
                if (agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue() == 2) {
                  InputBuilder builder = InputBuilder.create(agent.getInputOutput());
                  builder.add("retract-output", "*yes*");
                }
              }
            });
    agent
        .getEvents()
        .addListener(
            OutputEvent.class,
            new SoarEventListener() {

              @Override
              public void onEvent(SoarEvent event) {
                OutputEvent oe = (OutputEvent) event;

                if (oe.getMode() == OutputMode.MODIFIED_OUTPUT_COMMAND) {
                  outputs.add(Sets.newHashSet(oe.getWmes()));
                }
              }
            });

    sourceTestFile("testBasicOutput.soar");
    agent.runFor(3, RunType.DECISIONS);

    assertEquals(2, outputs.size());

    // Initially we'll have an output command with 5 WMEs, the output-link plus
    // the 4 created in our test production
    final Set<Wme> d1 = outputs.get(0);
    assertEquals(5, d1.size());
    final Identifier ol = agent.getInputOutput().getOutputLink();
    final SymbolFactory syms = agent.getSymbols();
    final Wme struct = Wmes.matcher(syms).id(ol).attr("struct").find(d1);
    assertNotNull(struct);
    assertNotNull(
        Wmes.matcher(syms)
            .id(struct.getValue().asIdentifier())
            .attr("name")
            .value("hello")
            .find(d1));
    assertNotNull(Wmes.matcher(syms).id(ol).attr("a").value(1).find(d1));
    assertNotNull(Wmes.matcher(syms).id(ol).attr("b").value(2).find(d1));

    // After retract-output is added in the second decision cycle above, the
    // test production will retract and we'll be left with only the output-link
    // wme
    final Set<Wme> d2 = outputs.get(1);
    assertEquals(1, d2.size());
  }

  @Test
  public void testGetPendingCommands() throws Exception {
    final InputOutput io = agent.getInputOutput();
    new CycleCountInput(io);
    sourceTestFile("testGetPendingCommands.soar");

    final MatcherBuilder m = Wmes.matcher(agent);

    agent.runFor(1, RunType.DECISIONS);
    assertEquals(1, io.getPendingCommands().size());
    assertEquals(1, Iterators.size(io.getOutputLink().getWmes()));
    assertNotNull(m.id(io.getOutputLink()).attr("first").find(io.getPendingCommands()));

    agent.runFor(1, RunType.DECISIONS);
    assertEquals(1, io.getPendingCommands().size());
    assertEquals(2, Iterators.size(io.getOutputLink().getWmes()));
    assertNotNull(m.id(io.getOutputLink()).attr("second").find(io.getPendingCommands()));

    agent.runFor(1, RunType.DECISIONS);
    assertEquals(1, io.getPendingCommands().size());
    assertEquals(3, Iterators.size(io.getOutputLink().getWmes()));
    assertNotNull(m.id(io.getOutputLink()).attr("third").find(io.getPendingCommands()));

    agent.runFor(1, RunType.DECISIONS);
    assertEquals(0, io.getPendingCommands().size());
    assertEquals(3, Iterators.size(io.getOutputLink().getWmes()));
  }
  /*
  @Test
  public void testInputIsRestoredAfterInitSoar() throws Exception
  {
      final InputOutput io = agent.getInputOutput();
      final ByRef<InputWme> wme1 = ByRef.create(null);
      final ByRef<InputWme> wme2 = ByRef.create(null);
      agent.getEventManager().addListener(InputEvent.class, new SoarEventListener() {

          @Override
          public void onEvent(SoarEvent event)
          {
              if(wme1.value == null)
              {
                  io.getSymbols().createIdentifier('S'); // dummy symbol to force use of idMap in restorePreviousInput()
                  final Identifier id = io.getSymbols().createIdentifier('S');
                  wme1.value = InputWmes.add(io, "some", id);
                  wme2.value = InputWmes.add(io, id, "test", 99);
              }
          }});
      agent.runFor(1, RunType.DECISIONS);
      assertNotNull(wme1.value);
      assertNotNull(wme2.value);

      agent.initialize();

      final Wme inner1 = Wmes.matcher(agent).id(io.getInputLink()).attr("some").find(io.getInputLink().getWmes());
      assertNotNull(inner1);
      assertSame(wme1.value, inner1.getAdapter(InputWme.class));

      final Wme inner2 = Wmes.matcher(agent).id(inner1.getValue().asIdentifier()).attr("test").value(99).find(inner1.getChildren());
      assertNotNull(inner2);
      assertSame(wme2.value, inner2.getAdapter(InputWme.class));
  }
  */

  @Test(expected = IllegalArgumentException.class)
  public void testAddInputWmeThrowsAnExceptionIfIdComesFromAnotherSymbolFactory() {
    final SymbolFactory syms = agent.getSymbols();
    final SymbolFactory other = new SymbolFactoryImpl();
    agent
        .getInputOutput()
        .addInputWme(other.createIdentifier('T'), syms.createString("hi"), syms.createInteger(99));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddInputWmeThrowsAnExceptionIfAttributeComesFromAnotherSymbolFactory() {
    final SymbolFactory syms = agent.getSymbols();
    final SymbolFactory other = new SymbolFactoryImpl();
    agent
        .getInputOutput()
        .addInputWme(syms.createIdentifier('T'), other.createString("hi"), syms.createInteger(99));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddInputWmeThrowsAnExceptionIfValueComesFromAnotherSymbolFactory() {
    final SymbolFactory syms = agent.getSymbols();
    final SymbolFactory other = new SymbolFactoryImpl();
    agent
        .getInputOutput()
        .addInputWme(syms.createIdentifier('T'), syms.createString("hi"), other.createInteger(99));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddInputWmeInternalThrowsExceptionIfIdIsNull() {
    InputOutputImpl io = new InputOutputImpl(mock(Agent.class));
    io.addInputWmeInternal(null, mock(Symbol.class), mock(Symbol.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddInputWmeInternalThrowsExceptionIfAttrIsNull() {
    InputOutputImpl io = new InputOutputImpl(mock(Agent.class));
    io.addInputWme(mock(Identifier.class), null, mock(Symbol.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddInputWmeInternalThrowsExceptionIfValueIsNull() {
    InputOutputImpl io = new InputOutputImpl(mock(Agent.class));
    io.addInputWme(mock(Identifier.class), mock(Symbol.class), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUpdateInputWmeInternalThrowsExceptionIfWmeIsNull() {
    InputOutputImpl io = new InputOutputImpl(mock(Agent.class));
    io.updateInputWme(null, mock(Symbol.class));
  }
}
