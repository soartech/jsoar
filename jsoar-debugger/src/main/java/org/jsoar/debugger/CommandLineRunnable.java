/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.util.concurrent.Callable;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;

/** @author ray */
public class CommandLineRunnable implements Callable<Void> {
  private final JSoarDebugger ifc;
  private final String command;

  /**
   * @param ifc The debugger object
   * @param command The command string to evaluate
   */
  public CommandLineRunnable(JSoarDebugger ifc, String command) {
    this.ifc = ifc;
    this.command = command;
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public Void call() {
    final Printer printer = ifc.getAgent().getAgent().getPrinter();
    printer.startNewLine().print(command).startNewLine();
    try {
      String result = ifc.getAgent().getInterpreter().eval(command);
      if (result != null && result.length() != 0) {
        printer.startNewLine().print(result).flush();
      }
    } catch (SoarException e) {
      printer.error(e.getMessage() + "\n");
    }
    printer.flush();
    ifc.update(false);
    return null;
  }
}
