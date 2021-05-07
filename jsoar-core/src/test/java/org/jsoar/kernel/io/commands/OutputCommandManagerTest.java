package org.jsoar.kernel.io.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OutputCommandManagerTest {
  private Agent agent;
  private OutputCommandManager outputManager;

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {
    this.agent = new Agent(false);

    this.outputManager = new OutputCommandManager(agent.getEvents());

    this.agent.setStopPhase(Phase.INPUT);
    this.agent.initialize();
    this.agent.getProperties().set(SoarProperties.WAITSNC, true);
  }

  /** @throws java.lang.Exception */
  @After
  public void tearDown() throws Exception {
    this.outputManager.dispose();
  }

  @Test
  public void testRegisterHandler() throws Exception {
    final AtomicBoolean added = new AtomicBoolean();
    final AtomicBoolean removed = new AtomicBoolean();
    final OutputCommandHandler handler =
        new OutputCommandHandler() {
          @Override
          public void onCommandRemoved(String commandName, Identifier commandId) {
            assertEquals("jump-high", commandName);
            removed.set(true);
          }

          @Override
          public void onCommandAdded(String commandName, Identifier commandId) {
            assertEquals("jump-high", commandName);
            Iterator<Wme> wmes = commandId.getWmes();
            assertTrue(wmes.hasNext());
            Wme wme = wmes.next();
            assertEquals("how-high", wme.getAttribute().asString().toString());
            assertEquals(5, wme.getValue().asInteger().getValue());
            added.set(true);
          }
        };
    outputManager.registerHandler("jump-high", handler);

    agent
        .getProductions()
        .loadProduction(
            "testRegisterHandler\n"
                + "(state <s> ^superstate nil ^io.output-link <ol> -^remove)"
                + "-->\n"
                + "(<ol> ^jump-high <jh>)\n"
                + "(<jh> ^how-high 5)\n"
                + "");

    agent.runFor(1, RunType.DECISIONS);
    assertTrue(added.get());
    assertFalse(removed.get());

    agent
        .getProductions()
        .loadProduction(
            "testRemove\n" + "(state <s> ^superstate nil)" + "-->\n" + "(<s> ^remove <id>)\n" + "");

    agent.runFor(1, RunType.DECISIONS);

    assertTrue(removed.get());
  }
}
