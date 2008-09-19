/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.Arguments;

/**
 * @author ray
 */
public class RhsFunctionManager
{
    private final SymbolFactory syms;
    private final Map<String, RhsFunctionHandler> handlers = new HashMap<String, RhsFunctionHandler>();
    
    
    /**
     * @param syms
     */
    public RhsFunctionManager(SymbolFactory syms)
    {
        this.syms = syms;
    }

    /**
     * Register a RHS function
     * 
     * @param handler The handler to call for the function
     */
    public void registerHandler(RhsFunctionHandler handler)
    {
        Arguments.checkNotNull(handler, "handler");
        
        handlers.put(handler.getName(), handler);
    }
    
    /**
     * Remove a RHS function previously registered with {@link #registerHandler(String, RhsFunctionHandler)}
     * 
     * @param name Name of handler to remove
     */
    public void unregisterHandler(String name)
    {
        Arguments.checkNotNull(name, "name");
        
        handlers.remove(name);
    }
    
    public Symbol execute(String name, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctionHandler handler = handlers.get(name);
        
        if(handler != null)
        {
            return handler.execute(syms, arguments);
        }
        
        throw new RhsFunctionException("No function '" + name + "' registered");
    }
}
