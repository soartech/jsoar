/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 11, 2008
 */
package org.jsoar.kernel.rhs.functions;

/**
 * @author ray
 */
public abstract class StandaloneRhsFunctionHandler extends AbstractRhsFunctionHandler
{
    
    /**
     * @param name
     */
    protected StandaloneRhsFunctionHandler(String name)
    {
        super(name);
    }
    
    /**
     * @param name
     * @param minArgs
     * @param maxArgs
     */
    protected StandaloneRhsFunctionHandler(String name, int minArgs, int maxArgs)
    {
        super(name, minArgs, maxArgs);
    }
    
    @Override
    public boolean mayBeStandalone()
    {
        return true;
    }
    
    @Override
    public boolean mayBeValue()
    {
        return false;
    }
    
}
