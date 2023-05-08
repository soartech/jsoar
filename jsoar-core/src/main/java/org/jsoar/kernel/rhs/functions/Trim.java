package org.jsoar.kernel.rhs.functions;

import java.util.List;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;

public class Trim extends AbstractRhsFunctionHandler 
{

    public Trim() 
    {
        super("trim", 1, 1);
    }

    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException 
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        StringSymbol strArg = arguments.get(0).asString();
        if (strArg == null) 
        {
            throw new RhsFunctionException(this.getName() + " was called with a non-string symbol argument in rule "
                    + context.getProductionBeingFired());
        }
        
        return context.getSymbols().createString(strArg.toString().trim());
    }

}
