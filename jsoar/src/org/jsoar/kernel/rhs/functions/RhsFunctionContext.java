/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 14, 2008
 */
package org.jsoar.kernel.rhs.functions;

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
}
