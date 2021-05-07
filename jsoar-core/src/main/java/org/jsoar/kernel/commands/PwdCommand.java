package org.jsoar.kernel.commands;

import java.util.concurrent.Callable;
import org.jsoar.util.commands.PicocliSoarCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "pwd" command.
 *
 * @author austin.brehob
 */
public class PwdCommand extends PicocliSoarCommand {
  public PwdCommand(SourceCommand sourceCommand) {
    super(new Pwd(sourceCommand));
  }

  @Override
  public Object getCommand() {

    return (Pwd) super.getCommand();
  }

  @Command(
      name = "pwd",
      description = "Prints the working directory to the screen",
      subcommands = {HelpCommand.class})
  public static class Pwd implements Callable<String> {
    private final SourceCommand sourceCommand;

    public Pwd(SourceCommand sourceCommand) {
      this.sourceCommand = sourceCommand;
    }

    @Override
    public String call() {
      return sourceCommand.getWorkingDirectory().replace('\\', '/');
    }
  }
}
