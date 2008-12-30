/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 14, 2008
 */
package org.jsoar.kernel.rhs.functions;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * Context interface passed to all RHS functions when they are executed. This
 * interface provides access to the symbol factory as well as methods that
 * allow RHS functions to safely add new WMEs to working memory.
 * 
 * @author ray
 */
public interface RhsFunctionContext
{
    /**
     * Returns the symbols factory that RHS functions should use to construct
     * new symbols.
     * 
     * @return the agent's symbol factory.
     */
    SymbolFactory getSymbols();
    
    /**
     * Add a WME from the RHS function. The WME is given whatever support and
     * preference type is given to the action containing the RHS function call.
     * 
     * @param id The identifier of the new WME
     * @param attr The attribute of the new WME
     * @param value The value of the new WME
     * @throws IllegalArgumentException if any of the parameters are <code>null</code>.
     */
    void addWme(Identifier id, Symbol attr, Symbol value);
    
    /**
     * Returns the production that is currently firing, causing the RHS function
     * to execute.
     * 
     * @return The production being fired.
     */
    Production getProductionBeingFired();
}
