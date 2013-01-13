 /*
 * Copyright (c) 2013 Soar Technology Inc.
 *
 * Created on January 07, 2013
 */
package org.jsoar.kernel.rhs.functions;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;

/**
 * 
 * Implementation of <b>cmd</b> RHS function from SML. Used to call built-in Soar commands.
 * 
 * <p><b>cmd</b> takes a variable number of arguments: the first being the command name
 * and the remaining the command's arguments. The return value is the output of the Soar command
 * (the contents that would otherwise be printed out to the agent trace if it were invoked
 * from, say, the Soar debugger.)
 * 
 * <p><b>cmd</b> only accepts the names of built-in Soar commands (like print), not
 * RHS functions (see <b>exec</b>: {@link ExecRhsFunction}).
 * 
 * For example, the following will print the object bound to &lt;s&gt; with a depth of 2:
 * 
 * <pre>
 * {@code
 * sp {
 *     ...
 *     -->
 *     (write (cmd print -d 2 &lt;s&gt;)) }
 * </pre>
 * 
 * @author charles.newton
 */
public class CmdRhsFunction extends AbstractRhsFunctionHandler
{
    private final SoarCommandInterpreter interp;

    public CmdRhsFunction(SoarCommandInterpreter interp)
    {
        super("cmd", 1, Integer.MAX_VALUE);
        this.interp = interp; 
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
//        final StringBuilder concat = new StringBuilder();
//        for(Symbol s : arguments)
//        {
//            concat.append(String.format(" %#s", s));
//        }
//        // Delete the leading space.
//        concat.deleteCharAt(0);
//        final String exp = concat.toString();
        
        try
        {
            String commandName = arguments.get(0).toString();
            List<String> commandArgs = mapSymbolListToStringList(arguments);
            
            SourceLocation srcLoc = context.getProductionBeingFired().getLocation();
            SoarCommand command = this.interp.getCommand(commandName, srcLoc);
            final SoarCommandContext commandContext = new DefaultSoarCommandContext(srcLoc);
            String result = command.execute(commandContext, commandArgs.toArray(new String[commandArgs.size()]));
            return context.getSymbols().createString(result);
//            final String result = this.interp.eval(exp);
//            return context.getSymbols().createString(result);
        }
        catch (SoarException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }
    
    private List<String> mapSymbolListToStringList(List<Symbol> symList)
    {
        ArrayList<String> stringList = new ArrayList<String>(symList.size());
        
        for(Symbol s : symList)
        {
            stringList.add(String.format("%#s", s));
        }
        
        return stringList;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeStandalone()
     */
    @Override
    public boolean mayBeStandalone()
    {
        return true;
    }

}
