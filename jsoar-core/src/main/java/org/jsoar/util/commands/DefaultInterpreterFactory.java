/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2010
 */
package org.jsoar.util.commands;

import org.jsoar.kernel.Agent;

/**
 * @author ray
 */
public class DefaultInterpreterFactory implements SoarCommandInterpreterFactory
{

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreterFactory#create(org.jsoar.kernel.Agent)
     */
    @Override
    public SoarCommandInterpreter create(Agent agent)
    {
        return new DefaultInterpreter(agent);
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreterFactory#getName()
     */
    @Override
    public String getName()
    {
        return "default";
    }

}
