/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 14, 2008
 */
package org.jsoar.kernel.rhs.functions;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Context interface passed to all RHS functions when they are executed. This
 * interface provides access to the symbol factory as well as methods that
 * allow RHS functions to safely add new WMEs to working memory.
 * 
 * @author ray
 * @see RhsFunctionManager
 * @see RhsFunctionHandler
 * @see WmeFactory
 */
public interface RhsFunctionContext extends WmeFactory<Void>
{
    /**
     * Add a WME from the RHS function. The WME is given whatever support and
     * preference type is given to the action containing the RHS function call.
     * 
     * @param id The identifier of the new WME
     * @param attr The attribute of the new WME
     * @param value The value of the new WME
     * @return nothing. The actual WME is not created until later
     * @throws IllegalArgumentException if any of the parameters are <code>null</code>.
     */
    @Override
    Void addWme(Identifier id, Symbol attr, Symbol value);
    
    /**
     * Returns the production that is currently firing, causing the RHS function
     * to execute.
     * 
     * @return The production being fired.
     */
    Production getProductionBeingFired();
    
}
