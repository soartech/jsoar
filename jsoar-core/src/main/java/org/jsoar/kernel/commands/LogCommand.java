package org.jsoar.kernel.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
    final var log = ((Log) this.picocliCommand);
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
    private static final String sourceLocationSeparator = ".";

    @Spec
    private CommandSpec spec; // injected by picocli

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
        arity = "1",
        description = "Enables or disables logging strictness",
        converter = BooleanTypeConverter.class)
    Optional<Boolean> strict;

    @Option(
        names = {"-E", "--echo"},
        description = "Sets logger echo mode to on, simple, or off")
    Optional<EchoMode> echo;

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
        description = "Sets the logging level to trace, debug, info, warn, or error")
    Optional<LogLevel> level;

    @Option(
        names = {"-S", "--source"},
        description = "Sets the logging source to disk, stack, or none")
    Optional<SourceLocationMethod> source;

    @Option(
        names = {"-A", "--abbreviate"},
        arity = "1",
        description = "Enables or disables logging abbreviation",
        converter = BooleanTypeConverter.class)
    Optional<Boolean> abbreviate;

    @Parameters(
        description =
            "The logger to enable/disable or send a message to, "
                + "the log level, and/or the message to log")
    String[] params;

    @Override
    public void run() {
      if (logToAdd != null) {
        // log --add <loggerName>
        addLogger(logToAdd);
      } else if (enable) {
        enableLog(true);
      } else if (disable) {
        enableLog(false);
      } else if (init) {
        // log --init
        logManager.init();
        agent.getPrinter().startNewLine().print("Log manager re-initialized.");
      } else if (strict.isPresent()) {
        setStrictMode(strict.get());
      } else if (abbreviate.isPresent()) {
        setAbbreviate(abbreviate.get());
      } else if (echo.isPresent()) {
        // log --echo on/simple/off
        var echoMode = echo.get();
        logManager.setEchoMode(echoMode);
        agent.getPrinter().startNewLine().print("Logger echo mode set to: " + echoMode);
      } else if (source.isPresent()) {
        // log --source disk/stack/none
        var sourceLocationMethod = source.get();
        logManager.setSourceLocationMethod(sourceLocationMethod);
        agent
            .getPrinter()
            .startNewLine()
            .print("Logger source location " + "method set to: " + sourceLocationMethod);
      } else if (level.isPresent()) {
        // log --level trace/debug/info/warn/error
        var logLevel = level.get();
        logManager.setLogLevel(logLevel);
        agent.getPrinter().startNewLine().print("Logger level set to: " + logLevel);
      } else if (params == null) {
        // log
        agent.getPrinter().startNewLine().print(logManager.getLoggerStatus());
      } else {
        // log <loggerName> <loggingLevel> <[message]>
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
      if (sourceLocationMethod.equals(SourceLocationMethod.stack)) {
        return getGoalStackLocation(abbreviate);
      } else if (sourceLocationMethod.equals(SourceLocationMethod.disk)) {
        return getSourceFileLocation(context, abbreviate);
      } else {
        return null;
      }
    }

    public String getGoalStackLocation(boolean abbreviate) {
      final var location = new StringBuilder();

      Iterator<Goal> it = agent.getGoalStack().iterator();
      if (it.hasNext()) {
        // location.append(getOperatorNameFromGoal(it.next()));
        String thisGoal = getOperatorNameFromGoal(it.next());
        if (!abbreviate || !it.hasNext()) {
          location.append(thisGoal);
        } else {
          location.append(thisGoal.charAt(0));
        }
        while (it.hasNext()) {
          location.append(sourceLocationSeparator);
          // location.append(getOperatorNameFromGoal(it.next()));
          thisGoal = getOperatorNameFromGoal(it.next());
          if (!abbreviate || !it.hasNext()) {
            location.append(thisGoal);
          } else {
            location.append(thisGoal.charAt(0));
          }
        }
      }

      return location.toString();
    }

    public String getSourceFileLocation(SoarCommandContext context, boolean abbreviate) {
      var sourceLocation = context.getSourceLocation();
      if (sourceLocation != DefaultSourceLocation.UNKNOWN) {
        String fileName = sourceLocation.getFile();
        if (fileName != null && !fileName.isEmpty()) {
          return collapseFileName(fileName, interpreter.getWorkingDirectory(), abbreviate);
        }
      }
      return null;
    }

    private static String getOperatorNameFromGoal(Goal g) {
      Symbol opName = g.getOperatorName();
      return opName == null ? "?" : opName.toString();
    }

    public static List<String> uberSplit(String file) throws IOException {
      List<String> result = new ArrayList<>();

      var f = new File(file).getCanonicalFile();

      result.add(f.getName());
      f = f.getParentFile();
      while (f != null) {
        String n = f.getName();
        if (!n.isEmpty()) {
          result.add(f.getName());
        }
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
        if (!cwdParts[marker].equals(fileParts[marker])) {
          break;
        }
      }

      var result = "";

      int diff = cwdParts.length - marker;
      if (diff > 0) {
        result += "^" + diff + sourceLocationSeparator;
      }

      for (int i = marker; i < fileParts.length - 1; ++i) {
        if (abbreviate) {
          result += fileParts[i].charAt(0);
        } else {
          result += fileParts[i];
        }
        result += sourceLocationSeparator;
      }
      result += fileParts[fileParts.length - 1];

      return result;
    }

    /**
     * Adds a logger with the given name
     */
    private void addLogger(final String logToAdd) {
      try {
        logManager.addLogger(logToAdd);
      } catch (LoggerException e) {
        throw new ParameterException(spec.commandLine(), e.getMessage(), e);
      }

      agent.getPrinter().startNewLine().print("Added logger: " + logToAdd);
    }

    private void enableLog(boolean enabled) {

      final String enabledAsText = enabled ? "enabled" : "disabled";

      if (params == null) {
        if (logManager.isActive() == enabled) {
          agent.getPrinter().startNewLine()
              .print("Logging already {}.", enabledAsText);
        } else {
          logManager.setActive(enabled);
          agent.getPrinter().startNewLine().print("Logging {}.", enabledAsText);
        }
      } else {
        try {
          String loggerName = params[0];

          if (enabled) {
            logManager.enableLogger(loggerName);
          } else {
            logManager.disableLogger(loggerName);
          }

          agent.getPrinter().startNewLine()
              .print("Logger [" + loggerName + "] {}.", enabledAsText);

        } catch (LoggerException e) {
          agent.getPrinter().startNewLine().print(e.getMessage());
        }
      }
    }

    private void setAbbreviate(boolean enabled) {
      logManager.setAbbreviate(enabled);
      agent.getPrinter().startNewLine()
          .print("Logger using {} paths.", enabled ? "abbreviated" : "full");
    }

    private void setStrictMode(boolean enabled) {
      if (logManager.isStrict() == enabled) {
        agent
            .getPrinter()
            .startNewLine()
            .print("Logger already in {} mode.", enabled ? "strict" : "non-strict");
      } else {
        logManager.setStrict(enabled);
        agent
            .getPrinter()
            .startNewLine()
            .print("Logger set to {} mode.", enabled ? "strict" : "non-strict");
      }
    }
  }
}
