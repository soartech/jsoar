/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.Symbol;

/**
 * Interface for the implementation of a RHS function.
 * 
 * @author ray
 * @see RhsFunctions
 * @see RhsFunctionManager
 * @see AbstractRhsFunctionHandler
 * @see StandaloneRhsFunctionHandler
 */
public interface RhsFunctionHandler
{
    /**
     * Returns the name of the function. This method must always return the 
     * same value as long as the handler is registered.
     * 
     * @return The name of the function, e.g. "write"
     */
    String getName();
    
    /**
     * @return Minimum number of arguments required by this function
     */
    int getMinArguments();
    
    /**
     * @return Maximum number of argument required by this function
     */
    int getMaxArguments();
    
    /**
     * @return true if this function can be called standalone on the RHS
     */
    boolean mayBeStandalone();
    
    /**
     * @return true if this function can be called as part of a value on the RHS.
     */
    boolean mayBeValue();
    
    /**
     * Execute the function and return a result.
     * 
     * @param context Context info for the function including symbol factory
     * @param arguments List of arguments
     * @return Result symbol
     * @throws RhsFunctionException if an error occurs
     */
    Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException;

}
