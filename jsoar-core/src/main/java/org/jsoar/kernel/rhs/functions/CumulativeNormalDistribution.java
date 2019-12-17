package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Implementation of the Cumulative Normal Distribution function. 
 * 
 * <p> This function takes one floating point argument and returns the
 * value of the cumulative distribution function for a normal distribution
 * with mean 0 and variance 1.
 * 
 * <p>For example:
 * <ul>
 * <li> {@code (cndf 0.0) returns 0.5}
 * <li> {@code (cndf 1.0) returns approximately 0.8413}
 * </ul>
 * 
 * @author chris.kawatsu
 *
 */
public class CumulativeNormalDistribution extends AbstractRhsFunctionHandler
{
    private final NormalDistribution nd;
    
    public CumulativeNormalDistribution()
    {
        super("cndf", 1, 1);
        nd = new NormalDistribution();
    }
    
    /*
     * (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
        final double value = arguments.get(0).asDouble().getValue();
        
        final double cndf = nd.cumulativeProbability(value);
        
        return context.getSymbols().createDouble(cndf);
    }
}
