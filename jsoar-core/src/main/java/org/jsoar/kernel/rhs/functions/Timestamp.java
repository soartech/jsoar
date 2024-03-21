package org.jsoar.kernel.rhs.functions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jsoar.kernel.symbols.Symbol;

/**
 * {@code (timestamp)} RHS function.
 * Returns a string timestamp for the current time in the same format used by log, i.e. yyyy-MM-dd HH:mm:ss.SSS
 * 
 * @author marinier
 */
public class Timestamp extends AbstractRhsFunctionHandler
{
    private final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Construct a new timestamp RHS function
     * generator.
     */
    public Timestamp()
    {
        super("timestamp", 0, 0);
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
        return context.getSymbols().createString(this.timestampFormatter.format(new Date(System.currentTimeMillis())));
    }
    
}
