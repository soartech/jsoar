/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Interface for the implementation of a RHS function
 * 
 * @author ray
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
     * Execute the function and return a result.
     * 
     * @param syms SymbolImpl factory to create result symbols with as necessary
     * @param arguments List of arguments
     * @return Result symbol
     * @throws RhsFunctionException if an error occurs
     */
    Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException;

}
