/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 21, 2010
 */
package org.jsoar.script;

import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;

import com.google.common.base.Joiner;

/**
 * Generic RHS function for scripting languages. Concatenates all args
 * together and evaluates the resulting string. Of course, escaping can
 * be an issue...
 * 
 * <p>For example:
 * <pre>
 * {@code (javascript |println("| <value> |")|)}
 * </pre>
 * 
 * @author ray
 */
public class ScriptRhsFunction extends AbstractRhsFunctionHandler
{
    private static final Joiner JOINER = Joiner.on("");
    
    private final ScriptEngineState state;
    
    /**
     * Construct RHS function with given name and engine
     * 
     * @param name the name of the function
     * @param state the engine
     */
    public ScriptRhsFunction(String name, ScriptEngineState state)
    {
        super(name, 1, Integer.MAX_VALUE);
        
        this.state = state;
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
        final String code = JOINER.join(arguments);
        try
        {
            final Object result = state.eval(code);
            
            return Symbols.create(context.getSymbols(), result);
        }
        catch(SoarException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }
}
