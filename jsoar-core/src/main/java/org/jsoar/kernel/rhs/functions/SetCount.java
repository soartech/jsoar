package org.jsoar.kernel.rhs.functions;

import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

public class SetCount extends AbstractRhsFunctionHandler
{
    
    public SetCount()
    {
        super("set-count", 1, 2);
    }
    
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        Identifier setId = arguments.get(0).asIdentifier();
        if(setId == null)
        {
            throw new RhsFunctionException(this.getName() + " was called with a non-identifer argument in rule " + context.getProductionBeingFired());
        }
        
        if(arguments.size() == 2)
        {
            Symbol targetAttr = arguments.get(1);
            
            long setSize = 0;
            Iterator<Wme> itr = setId.getWmes();
            while(itr.hasNext())
            {
                Wme w = itr.next();
                if(w.getAttribute().equals(targetAttr))
                {
                    setSize++;
                }
            }
            
            return context.getSymbols().createInteger(setSize);
            
        }
        
        return context.getSymbols().createInteger(0);
    }
    
}
