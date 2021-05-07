/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 29, 2010
 */
package org.jsoar.kernel.commands;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.properties.PropertyManager;
import org.junit.Before;
import org.junit.Test;

public class SetStopPhaseCommandTest {
  private PropertyManager props;
  private SoarSettingsCommand command;

  @Before
  public void setUp() throws Exception {
    Agent agent = new Agent();
    props = agent.getProperties();
    command = new SoarSettingsCommand(agent);
  }

  @Test(expected = SoarException.class)
  public void testThrowsExceptionOnUnknownOption() throws Exception {
    verify(null, "unknown");
  }

  // input -> propose -> decision -> apply -> output

  @Test
  public void testSetToAfterInput() throws Exception {
    verify(Phase.INPUT, "input");
  }

  @Test
  public void testSetToAfterPropose() throws Exception {
    verify(Phase.PROPOSE, "proposal");
  }

  @Test
  public void testSetToDecision() throws Exception {
    verify(Phase.DECISION, "decide");
  }

  @Test
  public void testSetToApply() throws Exception {
    verify(Phase.APPLY, "apply");
  }

  @Test
  public void testSetToOutput() throws Exception {
    verify(Phase.OUTPUT, "output");
  }

  private void verify(Phase expectedPhase, String... args) throws SoarException {
    final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
    argsList.add(0, "soar");
    argsList.add(1, "stop-phase");
    command.execute(DefaultSoarCommandContext.empty(), argsList.toArray(new String[] {}));
    assertSame(expectedPhase, props.get(SoarProperties.STOP_PHASE));
  }
}
