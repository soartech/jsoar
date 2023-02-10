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

public class LogRhsFunction extends AbstractRhsFunctionHandler
{
    
    private final SoarCommandInterpreter interp;
    
    public LogRhsFunction(SoarCommandInterpreter interp)
    {
        super("log", 0, Integer.MAX_VALUE);
        this.interp = interp;
    }
    
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        final String commandName = "log";
        List<String> commandArgs = mapSymbolListToStringList(arguments);
        commandArgs.add(0, commandName);
        commandArgs.add(1, "--collapse");
        
        try
        {
            SourceLocation srcLoc = context.getProductionBeingFired().getLocation();
            SoarCommand command = this.interp.getCommand(commandName, srcLoc);
            final SoarCommandContext commandContext = new DefaultSoarCommandContext(srcLoc);
            String result = command.execute(commandContext, commandArgs.toArray(new String[commandArgs.size()]));
            return context.getSymbols().createString(result);
        }
        catch(SoarException e)
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
    
    @Override
    public boolean mayBeStandalone()
    {
        return true;
    }
    
    @Override
    public boolean mayBeValue()
    {
        return false;
    }
}
