package org.jsoar.kernel.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.LogManager;
import org.jsoar.kernel.LogManager.EchoMode;
import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.LogManager.LoggerException;
import org.jsoar.kernel.LogManager.SourceLocationMethod;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.PicocliSoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * This is the implementation of the "log" command.
 *
 * @author austin.brehob
 */
public class LogCommand extends PicocliSoarCommand {

  public LogCommand(Agent agent, SoarCommandInterpreter interpreter) {
    super(agent, new Log(agent, interpreter, null));
  }

  @Override
  public String execute(SoarCommandContext context, String[] args) throws SoarException {
    final Log log = ((Log) this.picocliCommand);
    log.context = context;
    return super.execute(context, args);
  }

  @Command(
      name = "log",
      description = "Adjusts logging settings",
      subcommands = {HelpCommand.class})
  public static class Log implements Runnable {
    private final Agent agent;
    private final LogManager logManager;
    private final SoarCommandInterpreter interpreter;
    private SoarCommandContext context;
    private static String sourceLocationSeparator = ".";

    @Spec private CommandSpec spec; // injected by picocli

    public Log(Agent agent, SoarCommandInterpreter interpreter, SoarCommandContext context) {
      this.agent = agent;
      this.logManager = agent.getLogManager();
      this.interpreter = interpreter;
      this.context = context;
    }

    @Option(
        names = {"-a", "--add"},
        description = "Adds a logger with the given name")
    String logToAdd;

    @Option(
        names = {"-e", "--on", "--enable", "--yes"},
        defaultValue = "false",
        description = "Enables logging")
    boolean enable;

    @Option(
        names = {"-d", "--off", "--disable", "--no"},
        defaultValue = "false",
        description = "Disables logging")
    boolean disable;

    @Option(
        names = {"-s", "--strict"},
        description = "Enables or disables logging strictness")
    String strict;

    @Option(
        names = {"-E", "--echo"},
        description = "Sets logger echo mode to on, simple, or off")
    String echo;

    @Option(
        names = {"-i", "--init"},
        defaultValue = "false",
        description = "Re-initializes log manager")
    boolean init;

    @Option(
        names = {"-c", "--collapse"},
        defaultValue = "false",
        description = "Specifies collapsed logging")
    boolean collapse;

    @Option(
        names = {"-l", "--level"},
        description = "Sets the logging level to " + "trace, debug, info, warn, or error")
    String level;

    @Option(
        names = {"-S", "--source"},
        description = "Sets the logging source to disk, stack, or none")
    String source;

    @Option(
        names = {"-A", "--abbreviate"},
        description = "Enables or disables logging abbreviation")
    String abbreviate;

    @Parameters(
        description =
            "The logger to enable/disable or send a message to, "
                + "the log level, and/or the message to log")
    String[] params;

    @Override
    public void run() {
      // log --add <loggerName>
      if (logToAdd != null) {
        try {
          logManager.addLogger(logToAdd);
        } catch (LoggerException e) {
          throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }

        agent.getPrinter().startNewLine().print("Added logger: " + logToAdd);
      } else if (enable) {
        // log --enable
        if (params == null) {
          if (logManager.isActive()) {
            agent.getPrinter().startNewLine().print("Logging already enabled.");
          } else {
            logManager.setActive(true);
            agent.getPrinter().startNewLine().print("Logging enabled.");
          }
        }
        // log --enable <loggerName>
        else {
          try {
            logManager.enableLogger(params[0]);
          } catch (LoggerException e) {
            agent.getPrinter().startNewLine().print(e.getMessage());
            return;
          }
          agent.getPrinter().startNewLine().print("Logger [" + params[0] + "] enabled.");
        }
      } else if (disable) {
        // log --disable
        if (params == null) {
          if (!logManager.isActive()) {
            agent.getPrinter().startNewLine().print("Logging already disabled.");
          } else {
            logManager.setActive(false);
            agent.getPrinter().startNewLine().print("Logging disabled.");
          }
        }
        // log --disable <loggerName>
        else {
          try {
            logManager.disableLogger(params[0]);
          } catch (LoggerException e) {
            agent.getPrinter().startNewLine().print(e.getMessage());
            return;
          }
          agent.getPrinter().startNewLine().print("Logger [" + params[0] + "] disabled.");
        }
      }
      // log --init
      else if (init) {
        logManager.init();
        agent.getPrinter().startNewLine().print("Log manager re-initialized.");
      } else if (strict != null) {
        // log --strict enable
        if (strict.toLowerCase().equalsIgnoreCase("yes")
            || strict.toLowerCase().equalsIgnoreCase("enable")
            || strict.toLowerCase().equalsIgnoreCase("on")) {
          if (logManager.isStrict()) {
            agent.getPrinter().startNewLine().print("Logger already in strict mode.");
          } else {
            logManager.setStrict(true);
            agent.getPrinter().startNewLine().print("Logger set to strict mode.");
          }
        }
        // log --strict disable
        else if (strict.toLowerCase().equalsIgnoreCase("no")
            || strict.toLowerCase().equalsIgnoreCase("disable")
            || strict.toLowerCase().equalsIgnoreCase("off")) {
          if (!logManager.isStrict()) {
            agent.getPrinter().startNewLine().print("Logger already in non-strict mode.");
          } else {
            logManager.setStrict(false);
            agent.getPrinter().startNewLine().print("Logger set to non-strict mode.");
          }
        } else {
          throw new ParameterException(spec.commandLine(), "Expected one argument: on | off");
        }
      } else if (abbreviate != null) {
        // log --abbreviate enable
        if (abbreviate.toLowerCase().equalsIgnoreCase("yes")
            || abbreviate.toLowerCase().equalsIgnoreCase("enable")
            || abbreviate.toLowerCase().equalsIgnoreCase("on")) {
          logManager.setAbbreviate(true);
          agent.getPrinter().startNewLine().print("Logger using abbreviated paths.");
        }
        // log --abbreviate disable
        else if (abbreviate.toLowerCase().equalsIgnoreCase("no")
            || abbreviate.toLowerCase().equalsIgnoreCase("disable")
            || abbreviate.toLowerCase().equalsIgnoreCase("off")) {
          logManager.setAbbreviate(false);
          agent.getPrinter().startNewLine().print("Logger using full paths.");
        } else {
          throw new ParameterException(spec.commandLine(), "Expected one argument: on | off");
        }
      }
      // log --echo on/simple/off
      else if (echo != null) {
        EchoMode echoMode;

        try {
          echoMode = EchoMode.fromString(echo);
        } catch (IllegalArgumentException e) {
          throw new ParameterException(
              spec.commandLine(), "Expected one argument: on | simple | off", e);
        }

        logManager.setEchoMode(echoMode);
        agent.getPrinter().startNewLine().print("Logger echo mode set to: " + echoMode.toString());
      }
      // log --source disk/stack/none
      else if (source != null) {
        SourceLocationMethod sourceLocationMethod = null;

        try {
          sourceLocationMethod = SourceLocationMethod.fromString(source);
        } catch (IllegalArgumentException e) {
          throw new ParameterException(
              spec.commandLine(), "Expected one argument: disk | stack | none", e);
        }

        logManager.setSourceLocationMethod(sourceLocationMethod);
        agent
            .getPrinter()
            .startNewLine()
            .print("Logger source location " + "method set to: " + sourceLocationMethod.toString());
      }
      // log --level trace/debug/info/warn/error
      else if (level != null) {
        LogLevel logLevel;
        try {
          logLevel = LogLevel.fromString(level);
        } catch (IllegalArgumentException e) {
          throw new ParameterException(
              spec.commandLine(),
              "Expected one argument: " + "trace | debug | info | warn | error",
              e);
        }

        logManager.setLogLevel(logLevel);
        agent.getPrinter().startNewLine().print("Logger level set to: " + logLevel.toString());
      }
      // log
      else if (params == null) {
        agent.getPrinter().startNewLine().print(logManager.getLoggerStatus());
      }
      // log <loggerName> <loggingLevel> <[message]>
      else {
        String loggerName;
        LogLevel logLevel;
        List<String> parameters;

        if (params.length == 1) {
          throw new ParameterException(spec.commandLine(), "Unknown command: " + params[0]);
        }

        try {
          // Did the user omit the LOGGER-NAME?
          // If so, the first argument will by the log level.
          // So let's try to cast the first argument to a log level.
          logLevel = LogManager.LogLevel.fromString(params[0]);

          // The user omitted LOGGER-NAME (we know because we just properly parsed the log level).
          loggerName =
              getSourceLocation(
                  context, logManager.getAbbreviate(), logManager.getSourceLocationMethod());
          if (loggerName != null) {
            // Prevent strict mode from biting us.
            if (!logManager.hasLogger(loggerName)) {
              try {
                logManager.addLogger(loggerName);
              } catch (LoggerException e) {
                throw new ParameterException(spec.commandLine(), e.getMessage(), e);
              }
            }
          }

          if (loggerName == null) {
            loggerName = "default";
          }

          parameters = Arrays.asList(Arrays.copyOfRange(params, 1, params.length));
        } catch (IllegalArgumentException e) {
          // The user specified LOGGER-NAME.
          loggerName = params[0];

          try {
            // Make sure that the log-level is valid.
            logLevel = LogManager.LogLevel.fromString(params[1]);
          } catch (IllegalArgumentException ee) {
            throw new ParameterException(
                spec.commandLine(), "Unknown log-level value: " + params[1], ee);
          }

          parameters = Arrays.asList(Arrays.copyOfRange(params, 2, params.length));
        }

        // Log the message.
        try {
          logManager.log(loggerName, logLevel, parameters, collapse);
        } catch (LoggerException e) {
          throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
      }
    }

    public String getSourceLocation(
        SoarCommandContext context, boolean abbreviate, SourceLocationMethod sourceLocationMethod) {
      if (sourceLocationMethod.equals(SourceLocationMethod.stack))
        return getGoalStackLocation(abbreviate);
      else if (sourceLocationMethod.equals(SourceLocationMethod.disk))
        return getSourceFileLocation(context, abbreviate);
      else return null;
    }

    public String getGoalStackLocation(boolean abbreviate) {
      final StringBuffer location = new StringBuffer();

      Iterator<Goal> it = agent.getGoalStack().iterator();
      if (it.hasNext()) {
        // location.append(getOperatorNameFromGoal(it.next()));
        String thisGoal = getOperatorNameFromGoal(it.next());
        if (!abbreviate || !it.hasNext()) location.append(thisGoal);
        else location.append(thisGoal.charAt(0));
        while (it.hasNext()) {
          location.append(sourceLocationSeparator);
          // location.append(getOperatorNameFromGoal(it.next()));
          thisGoal = getOperatorNameFromGoal(it.next());
          if (!abbreviate || !it.hasNext()) location.append(thisGoal);
          else location.append(thisGoal.charAt(0));
        }
      }

      return location.toString();
    }

    public String getSourceFileLocation(SoarCommandContext context, boolean abbreviate) {
      SourceLocation sourceLocation = context.getSourceLocation();
      if (sourceLocation != DefaultSourceLocation.UNKNOWN) {
        String fileName = sourceLocation.getFile();
        if (fileName != null && !fileName.isEmpty())
          return collapseFileName(fileName, interpreter.getWorkingDirectory(), abbreviate);
      }
      return null;
    }

    private static String getOperatorNameFromGoal(Goal g) {
      Symbol opName = g.getOperatorName();
      return opName == null ? "?" : opName.toString();
    }

    public static List<String> uberSplit(String file) throws IOException {
      List<String> result = new ArrayList<String>();

      File f = new File(file).getCanonicalFile();

      result.add(f.getName());
      f = f.getParentFile();
      while (f != null) {
        String n = f.getName();
        if (!n.isEmpty()) result.add(f.getName());
        f = f.getParentFile();
      }

      Collections.reverse(result);

      return result;
    }

    public static String collapseFileName(String file, String cwd, boolean abbreviate) {
      String[] cwdParts;
      String[] fileParts;

      try {
        cwdParts = uberSplit(cwd).toArray(new String[0]);
        fileParts = uberSplit(file).toArray(new String[0]);
      } catch (IOException e) {
        return null;
      }

      int minLength = Math.min(cwdParts.length, fileParts.length);

      int marker;
      for (marker = 0; marker < minLength; ++marker) {
        if (!cwdParts[marker].equals(fileParts[marker])) break;
      }

      String result = "";

      int diff = cwdParts.length - marker;
      if (diff > 0) result += "^" + diff + sourceLocationSeparator;

      for (int i = marker; i < fileParts.length - 1; ++i) {
        if (abbreviate) result += fileParts[i].charAt(0);
        else result += fileParts[i];
        result += sourceLocationSeparator;
      }
      result += fileParts[fileParts.length - 1];

      return result;
    }
  }
}
