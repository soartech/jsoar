/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2010
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandInterpreterFactory;

/**
 * @author ray
 */
public class SoarTclInterfaceFactory implements SoarCommandInterpreterFactory
{

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreterFactory#create(org.jsoar.kernel.Agent)
     */
    @Override
    public SoarCommandInterpreter create(Agent agent)
    {
        return SoarTclInterface.findOrCreate(agent);
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreterFactory#getName()
     */
    @Override
    public String getName()
    {
        return "tcl";
    }

}
