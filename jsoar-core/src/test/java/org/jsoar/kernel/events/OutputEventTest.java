/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 23, 2010
 */
package org.jsoar.kernel.events;

import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.io.OutputChange;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class OutputEventTest {
  private Agent agent;

  @Before
  public void setUp() throws Exception {
    agent = new Agent(getClass().getName());
  }

  @After
  public void tearDown() throws Exception {
    if (agent != null) {
      agent.dispose();
    }
  }

  /** Test method for {@link org.jsoar.kernel.events.OutputEvent#getChanges()}. */
  @Test(timeout = 10000)
  public void testGetChanges() throws Exception {
    // agent.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
    // Source test code with agent that adds/removes stuff from OL.
    agent.getInterpreter().source(getClass().getResource("OutputEventTest_testGetChanges.soar"));

    // Set up listener to cache changes during run
    final List<List<OutputChange>> changes = new ArrayList<List<OutputChange>>();
    final AtomicReference<Identifier> stuffId = new AtomicReference<Identifier>();
    agent
        .getEvents()
        .addListener(
            OutputEvent.class,
            new SoarEventListener() {
              @Override
              public void onEvent(SoarEvent event) {
                final OutputEvent oe = (OutputEvent) event;
                // Catch the id of the ^stuff attribute so we can use it in tests below.
                if (changes.size() == 0) {
                  stuffId.set(
                      oe.getOutputValue(
                              oe.getInputOutput().getOutputLink(),
                              oe.getInputOutput().getSymbols().createString("stuff"))
                          .asIdentifier());
                }
                changes.add(Lists.newArrayList(oe.getChanges()));
              }
            });
    // Run the agent until halt. Note the timeout on the test in case this goes badly.
    agent.runForever();

    assertNotNull(stuffId.get());

    // For each decision check that we got the additions and removals we expect.
    // To know what to expect, run the agent and watch the trace. The agent prints
    // out adds and removes. Everything below is a copy of that trace output

    // Decision 1
    validateChanges(
        changes.get(0),
        // additions
        triples(agent.getInputOutput().getOutputLink(), "stuff", stuffId.get()),
        // removals
        triples());

    // Decision 2
    validateChanges(
        changes.get(1),
        // additions
        triples(stuffId.get(), 1, 1),
        // removals
        triples());

    // Decision 3
    validateChanges(
        changes.get(2),
        // additions
        triples(stuffId.get(), 2, 1, stuffId.get(), 2, 2),
        // removals
        triples());

    // Decision 4
    validateChanges(
        changes.get(3),
        // additions
        triples(stuffId.get(), 3, 1, stuffId.get(), 3, 2, stuffId.get(), 3, 3),
        // removals
        triples(stuffId.get(), 1, 1, stuffId.get(), 2, 1));

    // Decision 5
    validateChanges(
        changes.get(4),
        // additions
        triples(stuffId.get(), 4, 1, stuffId.get(), 4, 2, stuffId.get(), 4, 3, stuffId.get(), 4, 4),
        // removals
        triples(stuffId.get(), 3, 1));

    // Decision 6
    validateChanges(
        changes.get(5),
        // additions
        triples(
            stuffId.get(),
            5,
            1,
            stuffId.get(),
            5,
            2,
            stuffId.get(),
            5,
            3,
            stuffId.get(),
            5,
            4,
            stuffId.get(),
            5,
            5),
        // removals
        triples(
            stuffId.get(), 2, 2, stuffId.get(), 3, 2, stuffId.get(), 4, 1, stuffId.get(), 4, 2));

    // Agent halts before output phase on decision 7, so we're done.
    assertEquals(6, changes.size());
  }

  private Object[][] triples(Object... args) {
    assertEquals(0, args.length % 3);
    final Object[][] result = new Object[args.length / 3][];
    for (int i = 0; i < args.length; i += 3) {
      result[i / 3] = new Object[] {args[i], args[i + 1], args[i + 2]};
    }
    return result;
  }

  private void validateChanges(List<OutputChange> changes, Object[][] added, Object[][] removed) {
    assertEquals(added.length + removed.length, changes.size());
    for (Object[] triple : added) {
      validateChange(changes, true, triple);
    }
    for (Object[] triple : removed) {
      validateChange(changes, false, triple);
    }
  }

  private void validateChange(List<OutputChange> changes, boolean add, Object[] triple) {
    assertEquals(3, triple.length);

    final Identifier id = (Identifier) triple[0];
    final Symbol attr = Symbols.create(agent.getSymbols(), triple[1]);
    final Symbol value = Symbols.create(agent.getSymbols(), triple[2]);

    // Find the triple (and add/remove flag) in change set
    for (OutputChange c : changes) {
      final Wme w = c.getWme();
      if (add == c.isAdded()
          && id == w.getIdentifier()
          && attr == w.getAttribute()
          && value == w.getValue()) {
        return;
      }
    }

    // Fail if not found
    fail(
        String.format(
            "Expected to find '(%s ^%s %s) %s' in change set", id, attr, value, add ? "+" : "-"));
  }
}
