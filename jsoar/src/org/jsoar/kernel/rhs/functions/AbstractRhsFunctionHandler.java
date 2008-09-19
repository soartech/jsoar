/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;



/**
 * @author ray
 */
public abstract class AbstractRhsFunctionHandler implements RhsFunctionHandler
{
    private final String name;
    
    /**
     * @param name
     */
    public AbstractRhsFunctionHandler(String name)
    {
        this.name = name;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.RhsFunctionHandler#getName()
     */
    @Override
    public String getName()
    {
        return name;
    }
}
