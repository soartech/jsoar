/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.debugger;

import java.lang.reflect.InvocationTargetException;
import org.jsoar.util.commands.PicocliSoarCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** @author ray */
public class LoadPluginCommand extends PicocliSoarCommand {
  /** @param debugger */
  public LoadPluginCommand(JSoarDebugger debugger) {
    super(null, new LoadPlugin(debugger));
  }

  @Command(
      name = "load-plugin",
      description = "Loads a debugger plugin",
      subcommands = {HelpCommand.class})
  public static class LoadPlugin implements Runnable {

    private final JSoarDebugger debugger;

    @Spec private CommandSpec spec;

    @Parameters(
        index = "0",
        description = "Fully qualified class name of plugin. Must be on the classpath.")
    private String className;

    @Parameters(index = "1..*", description = "Zero or more arguments expected by the plugin.")
    private String[] args = {};

    public LoadPlugin(JSoarDebugger debugger) {
      this.debugger = debugger;
    }

    /* (non-Javadoc)
     * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
     */
    @Override
    public void run() {
      try {
        Class<?> klass = Class.forName(className);
        JSoarDebuggerPlugin plugin;
        try {
          plugin = (JSoarDebuggerPlugin) klass.getConstructor().newInstance();
        } catch (IllegalArgumentException
            | InvocationTargetException
            | NoSuchMethodException
            | SecurityException e) {
          throw new ParameterException(spec.commandLine(), "Failed to instantiate plugin", e);
        }

        String[] initArgs = new String[args.length];
        for (int i = 0; i < args.length; ++i) {
          initArgs[i] = args[i].toString();
        }

        plugin.initialize(debugger, initArgs);
        debugger.addPlugin(plugin);
      } catch (ClassNotFoundException e) {
        throw new ParameterException(
            spec.commandLine(),
            "Failed to find plugin class. Maybe it's not on the class path? : " + e.getMessage(),
            e);
      } catch (InstantiationException | IllegalAccessException | ClassCastException e) {
        throw new ParameterException(spec.commandLine(), e.getMessage(), e);
      }
    }
  }
}
