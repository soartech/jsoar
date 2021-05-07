package org.jsoar.kernel.commands;

import java.util.concurrent.Callable;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.util.commands.PicocliSoarCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * This is the implementation of the "handler" command.
 *
 * @author austin.brehob
 */
public class HandlerCommand extends PicocliSoarCommand {

  public HandlerCommand(Agent agent) {
    super(agent, new Handler(agent));
  }

  @Command(
      name = "handler",
      description = "Prints, enables, or disables RHS functions",
      subcommands = {HelpCommand.class})
  public static class Handler implements Callable<String> {
    private Agent agent;

    public Handler(Agent agent) {
      this.agent = agent;
    }

    @Option(
        names = {"on", "-e", "--on", "--enable"},
        description = "Enables RHS function")
    String functionToEnable;

    @Option(
        names = {"off", "-d", "--off", "--disable"},
        description = "Disables timers")
    String functionToDisable;

    @Override
    public String call() {
      RhsFunctionManager rhsFunctionManager = agent.getRhsFunctions();

      if (functionToEnable != null) {
        rhsFunctionManager.enableHandler(functionToEnable);
        return "RHS function enabled: " + functionToEnable;
      } else if (functionToDisable != null) {
        rhsFunctionManager.disableHandler(functionToDisable);
        return "RHS function disabled: " + functionToDisable;
      } else {
        String result = "===== Disabled RHS Functions =====\n";
        for (RhsFunctionHandler handler : rhsFunctionManager.getDisabledHandlers()) {
          result += handler.getName() + "\n";
        }
        return result;
      }
    }
  }
}
