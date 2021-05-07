/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 23, 2010
 */
package org.jsoar.soarunit;

import java.nio.file.Paths;
import org.jsoar.kernel.Agent;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.PicocliSoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;

/** @author ray */
public class SoarUnitCommand extends PicocliSoarCommand {
  public SoarUnitCommand(Agent agent) {
    super(
        agent,
        new SoarUnit(
            () -> agent.getPrinter().asPrintWriter(),
            () -> Paths.get(agent.getInterpreter().getWorkingDirectory())));
  }

  public static final String NAME = "soarunit";

  public static class Provider implements SoarCommandProvider {
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandProvider#registerCommands(org.jsoar.util.commands.SoarCommandInterpreter)
     */
    @Override
    public void registerCommands(SoarCommandInterpreter interp, Adaptable context) {
      interp.addCommand(NAME, new SoarUnitCommand(Adaptables.adapt(context, Agent.class)));
    }
  }
}
