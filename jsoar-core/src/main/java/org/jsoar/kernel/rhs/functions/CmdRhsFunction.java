/*
 * Copyright (c) 2013 Soar Technology Inc.
 *
 * Created on January 07, 2013
 */
package org.jsoar.kernel.rhs.functions;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.commands.ParsedCommand;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;

/**
 * Implementation of <b>cmd</b> RHS function from SML. Used to call built-in Soar commands.
 *
 * <p><b>cmd</b> takes a variable number of arguments: the first being the command name and the
 * remaining the command's arguments. The return value is the output of the Soar command (the
 * contents that would otherwise be printed out to the agent trace if it were invoked from, say, the
 * Soar debugger.)
 *
 * <p><b>cmd</b> only accepts the names of built-in Soar commands (like print), not RHS functions
 * (see <b>exec</b>: {@link ExecRhsFunction}).
 *
 * <p>For example, the following will print the object bound to {@code <s>} with a depth of 2:
 *
 * <pre>{@code
 * sp {
 *     ...
 *     -->
 *     (write (cmd print -d 2 <s>)) }
 * }</pre>
 *
 * @author charles.newton
 */
public class CmdRhsFunction extends AbstractRhsFunctionHandler {
  private final SoarCommandInterpreter interp;
  private final Agent agent;

  public CmdRhsFunction(SoarCommandInterpreter interp, Agent agent) {
    super("cmd", 1, Integer.MAX_VALUE);
    this.interp = interp;
    this.agent = agent;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
   */
  @Override
  public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
      throws RhsFunctionException {
    RhsFunctions.checkArgumentCount(this, arguments);

    try {
      String commandName = arguments.get(0).toString();
      List<String> commandArgs = mapSymbolListToStringList(arguments);

      SourceLocation srcLoc = context.getProductionBeingFired().getLocation();
      ParsedCommand parsedCommand = this.interp.getParsedCommand(commandName, srcLoc);

      // get the parsed command, which includes any args that are part of an alias, and add the
      // remaining args
      SoarCommand command = this.interp.getCommand(parsedCommand.getArgs().get(0), srcLoc);
      List<String> fullCommandArgs = parsedCommand.getArgs();
      fullCommandArgs.addAll(commandArgs.subList(1, commandArgs.size()));

      final SoarCommandContext commandContext = new DefaultSoarCommandContext(srcLoc);

      // we are using a string writer to intercept whatever printing happens so that the example
      // (write (crlf) (print <s> -d 2))
      // actually works like csoar (i.e., you have call write for it to appear
      // the actual returned result gets concatenated at the end. This should match how the command
      // output looks when executed normally
      // while still matching csoar's behavior.
      Printer printer = this.agent.getPrinter();
      StringWriter stringWriter = new StringWriter();
      printer.pushWriter(stringWriter);
      String result =
          command.execute(
              commandContext, fullCommandArgs.toArray(new String[fullCommandArgs.size()]));
      printer.popWriter();
      result = stringWriter.toString() + result;
      return context.getSymbols().createString(result);
    } catch (SoarException e) {
      throw new RhsFunctionException(e.getMessage(), e);
    }
  }

  private List<String> mapSymbolListToStringList(List<Symbol> symList) {
    ArrayList<String> stringList = new ArrayList<String>(symList.size());

    for (Symbol s : symList) {
      stringList.add(String.format("%#s", s));
    }

    return stringList;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeStandalone()
   */
  @Override
  public boolean mayBeStandalone() {
    return true;
  }
}
