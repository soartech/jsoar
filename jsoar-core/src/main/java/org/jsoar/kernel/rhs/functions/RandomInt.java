/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import java.util.Random;

import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * {@code (random-int)} RHS function.
 * 
 * <p>Has the following usage:
 * <ul>
 * <li>{@code (random-int)} - No args, returns a random integer in {@code [-Integer.MIN_VALUE, Integer.MAX_VALUE)}.
 * <li>{@code (random-int N)} - returns a random integer in {@code [0, N)} if N is positive and
 * a random integer in {@code (N, 0]} if N is negative.
 * </ul>
 * 
 * @author ray
 * @see Random#nextInt(int)
 */
public class RandomInt extends AbstractRhsFunctionHandler
{
    private final Random random;
    
    /**
     * Construct a new random int RHS function and use the given random number
     * generator.
     * 
     * @param random the random number generator to use
     */
    public RandomInt(Random random)
    {
        super("random-int", 0, 1);
        
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
        
        if(arguments.size() == 0)
        {
            return context.getSymbols().createInteger(random.nextInt());
        }
        else
        {
            final IntegerSymbol maxSym = arguments.get(0).asInteger();
            if(maxSym == null)
            {
                throw new RhsFunctionException("random-int: Expected integer for first argument, got " + arguments.get(0));
            }
            final long max = maxSym.getValue();
            if(max >= 0)
            {
                return context.getSymbols().createInteger(random.nextInt((int)max));
            }
            else
            {
                return context.getSymbols().createInteger(-random.nextInt(-(int) max));
            }
        }
    }

}
