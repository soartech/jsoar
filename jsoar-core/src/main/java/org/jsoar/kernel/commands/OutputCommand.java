package org.jsoar.kernel.commands;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.PrintCommand.Print;
import org.jsoar.util.TeeWriter;
import org.jsoar.util.commands.PicocliSoarCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/**
 * This is the implementation of the "output" command.
 *
 * @author austin.brehob
 */
public final class OutputCommand extends PicocliSoarCommand {

  public OutputCommand(Agent agent, Print printCommand) {
    super(agent, new Output(agent, printCommand, new LinkedList<Writer>()));
  }

  @Command(
      name = "output",
      description = "Commands related to handling output",
      subcommands = {
        HelpCommand.class,
        OutputCommand.Log.class,
        OutputCommand.PrintDepth.class,
        OutputCommand.Warnings.class
      })
  public static class Output implements Runnable {
    private Agent agent;
    private Print printCommand;
    private LinkedList<Writer> writerStack;

    public Output(Agent agent, Print printCommand, LinkedList<Writer> writerStack) {
      this.agent = agent;
      this.printCommand = printCommand;
      this.writerStack = writerStack;
    }

    // TODO Provide summary
    @Override
    public void run() {
      agent
          .getPrinter()
          .startNewLine()
          .print(
              "=======================================================\n"
                  + "-                    Output Status                    -\n"
                  + "=======================================================\n");
    }
  }

  @Command(
      name = "log",
      description = "Changes output log settings",
      subcommands = {HelpCommand.class})
  public static class Log implements Runnable {
    @ParentCommand Output parent; // injected by picocli

    @Option(
        names = {"-c", "--close"},
        arity = "0..1",
        defaultValue = "false",
        description = "Closes the log file")
    boolean close;

    @Parameters(index = "0", arity = "0..1", description = "File name")
    String fileName;

    @Override
    public void run() {
      if (close) {
        if (parent.writerStack.isEmpty()) {
          parent.agent.getPrinter().startNewLine().print("Log is not open.");
        } else {
          parent.writerStack.pop();
          parent.agent.getPrinter().popWriter();
          parent.agent.getPrinter().startNewLine().print("Log file closed.");
        }
      } else if (fileName != null) {
        if (fileName.equals("stdout")) {
          Writer w = new OutputStreamWriter(System.out);
          parent.writerStack.push(null);
          parent
              .agent
              .getPrinter()
              .pushWriter(new TeeWriter(parent.agent.getPrinter().getWriter(), w));
          parent.agent.getPrinter().startNewLine().print("Now writing to System.out");
        } else if (fileName.equals("stderr")) {
          Writer w = new OutputStreamWriter(System.err);
          parent.writerStack.push(null);
          parent
              .agent
              .getPrinter()
              .pushWriter(new TeeWriter(parent.agent.getPrinter().getWriter(), w));
          parent.agent.getPrinter().startNewLine().print("Now writing to System.err");
        } else {
          try {
            Writer w = new FileWriter(fileName);
            parent.writerStack.push(w);
            parent
                .agent
                .getPrinter()
                .pushWriter(new TeeWriter(parent.agent.getPrinter().getWriter(), w));
            // adding a newline at the end because we don't want the next line (which could be a
            // command output) to start on the same line.
            // normally we would leave this up to the debugger or other display mechanism to figure
            // out, but in this case it's going straight to a file.
            parent
                .agent
                .getPrinter()
                .startNewLine()
                .print("Log file " + fileName + " open.")
                .startNewLine();
          } catch (IOException e) {
            parent
                .agent
                .getPrinter()
                .startNewLine()
                .print("Failed to open file '" + fileName + "': " + e.getMessage());
          }
        }
      } else {
        parent
            .agent
            .getPrinter()
            .startNewLine()
            .print("log is " + (!parent.writerStack.isEmpty() ? "on" : "off"));
      }
    }
  }

  @Command(
      name = "print-depth",
      description = "Adjusts or displays the print-depth",
      subcommands = {HelpCommand.class})
  public static class PrintDepth implements Runnable {
    @ParentCommand Output parent; // injected by picocli

    @Spec CommandSpec spec; // injected by picocli

    @Parameters(index = "0", arity = "0..1", description = "New print depth")
    Integer printDepth;

    @Override
    public void run() {
      if (printDepth == null) {
        parent
            .agent
            .getPrinter()
            .startNewLine()
            .print("print-depth is " + Integer.toString(parent.printCommand.getDefaultDepth()));
      } else {
        int depth = Integer.valueOf(printDepth);
        try {
          parent.printCommand.setDefaultDepth(depth);
        } catch (SoarException e) {
          throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
        parent.agent.getPrinter().startNewLine().print("print-depth is now " + depth);
      }
    }
  }

  @Command(
      name = "warnings",
      description = "Toggles output warnings",
      subcommands = {HelpCommand.class})
  public static class Warnings implements Runnable {
    @ParentCommand Output parent; // injected by picocli

    @Option(
        names = {"on", "-e", "--on", "--enable"},
        defaultValue = "false",
        description = "Enables output warnings")
    boolean enable;

    @Option(
        names = {"off", "-d", "--off", "--disable"},
        defaultValue = "false",
        description = "Disables output warnings")
    boolean disable;

    @Override
    public void run() {
      if (!enable && !disable) {
        parent
            .agent
            .getPrinter()
            .print("warnings is " + (parent.agent.getPrinter().isPrintWarnings() ? "on" : "off"));
      } else if (enable) {
        parent.agent.getPrinter().setPrintWarnings(true);
        parent.agent.getPrinter().print("warnings is now on");
      } else {
        parent.agent.getPrinter().setPrintWarnings(false);
        parent.agent.getPrinter().print("warnings is now off");
      }
    }
  }
}
