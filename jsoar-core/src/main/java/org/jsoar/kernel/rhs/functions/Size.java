package org.jsoar.kernel.rhs.functions;

import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

public class Size extends AbstractRhsFunctionHandler 
{

    public Size() 
    {
        super("size", 1, 1);
    }

    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException 
    {
        RhsFunctions.checkArgumentCount(this, arguments);

        Identifier sizeId = arguments.get(0).asIdentifier();
        if (sizeId == null) 
        {
            throw new RhsFunctionException(this.getName() + " was called with a non-identifer argument in rule "
                    + context.getProductionBeingFired());
        }
        
        long sizeCount = 0;
        Iterator<Wme> itr = sizeId.getWmes();
        while(itr.hasNext())
        {
            itr.next();
            sizeCount++;
        }
        return context.getSymbols().createInteger(sizeCount);
    }

}
