/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import java.util.Random;

import org.jsoar.kernel.symbols.Symbol;

/**
 * {@code (random-float)} RHS function. This is deprecated, use rand-float instead for better csoar compatibility.
 * 
 * <p>Has the following usage:
 * <ul>
 * <li>{@code (random-float)} - No args, returns a random double in {@code [0.0, 1.0)}.
 * </ul>
 * 
 * @author ray
 * @see Random#nextDouble()
 */
public class RandomFloat extends AbstractRhsFunctionHandler
{
    private final Random random;
    
    /**
     * Construct a new random int RHS function and use the given random number
     * generator.
     * 
     * @param random the random number generator to use
     */
    public RandomFloat(Random random)
    {
        super("random-float", 0, 0);
        
        this.random = random;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        return context.getSymbols().createDouble(random.nextDouble());
    }

}
