package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

import com.google.common.collect.Streams;

public class Size extends AbstractRhsFunctionHandler {

    public Size() {
        super("size", 1, 1);
    }

    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException {
        RhsFunctions.checkArgumentCount(this, arguments);

        Identifier sizeId = arguments.get(0).asIdentifier();
        if (sizeId == null) {
            throw new RhsFunctionException(this.getName() + " was called with a non-identifer argument in rule "
                    + context.getProductionBeingFired());
        }

        long sizeCount = Streams.stream(sizeId.getWmes()).count();
        return context.getSymbols().createInteger(sizeCount);
    }

}
