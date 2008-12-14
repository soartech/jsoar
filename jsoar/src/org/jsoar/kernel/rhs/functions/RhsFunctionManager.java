/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.util.Arguments;

/**
 * @author ray
 */
public class RhsFunctionManager
{
    private final SymbolFactoryImpl syms;
    private final Map<String, RhsFunctionHandler> handlers = new HashMap<String, RhsFunctionHandler>();
    
    
    /**
     * @param syms
     */
    public RhsFunctionManager(SymbolFactoryImpl syms)
    {
        this.syms = syms;
    }

    /**
     * Returns a list of all regsitered RHS function handlers. The list is
     * a copy and may be modified by the caller.
     * 
     * @return Copy of list of all regsitered RHS function handlers 
     */
    public List<RhsFunctionHandler> getHandlers()
    {
        return new ArrayList<RhsFunctionHandler>(handlers.values());
    }
    
    /**
     * Register a RHS function
     * 
     * @param handler The handler to call for the function
     * @return The previosly registered handler
     */
    public RhsFunctionHandler registerHandler(RhsFunctionHandler handler)
    {
        Arguments.checkNotNull(handler, "handler");
        
        return handlers.put(handler.getName(), handler);
    }
    
    /**
     * Lookup a registered handler by name
     * 
     * @param name The name of the handler
     * @return The handler or null if none is registered
     */
    public RhsFunctionHandler getHandler(String name)
    {
        return handlers.get(name);
    }
    
    /**
     * Remove a RHS function previously registered with {@link #registerHandler(RhsFunctionHandler)}
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
