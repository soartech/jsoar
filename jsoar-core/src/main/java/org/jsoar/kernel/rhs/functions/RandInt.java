package org.jsoar.kernel.rhs.functions;

import java.util.List;
import java.util.Random;

import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * {@code (rand-int)} RHS function. Use this instead of random-int for csoar compatibility.
 * 
 * <p>Has the following usage:
 * <ul>
 * <li>{@code (rand-int)} - No args, returns a random integer in {@code [-Integer.MIN_VALUE, Integer.MAX_VALUE)}.
 * <li>{@code (rand-int N)} - returns a random integer in {@code [0, N]} if N is positive and
 * a random integer in {@code [N, 0]} if N is negative.
 * </ul>
 * 
 * <p> Csoar compatibility notes:
 * <ul>
 * <li> When no argument is supplied, csoar will generate values in {@code [-2^31, 2^31]} (note the inclusive endpoint).
 * <li> Csoar doesn't properly support negative arguments like jsoar does (it seems to wrap them around to large positive numbers).
 * </ul>
 * 
 * @author marinier
 * @see Random#nextInt(int)
 */
public class RandInt extends AbstractRhsFunctionHandler
{
    private final Random random;
    
    /**
     * Construct a new random int RHS function and use the given random number
     * generator.
     * 
     * @param random the random number generator to use
     */
    public RandInt(Random random)
    {
        super("rand-int", 0, 1);
        
        this.random = random;
    }
    
    /*
     * (non-Javadoc)
     * 
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
                throw new RhsFunctionException("rand-int: Expected integer for first argument, got " + arguments.get(0));
            }
            final long max = maxSym.getValue();
            if(max >= 0)
            {
                return context.getSymbols().createInteger(random.nextInt(((int) max) + 1));
            }
            else
            {
                return context.getSymbols().createInteger(-random.nextInt((-(int) max) + 1));
            }
        }
    }
    
}
