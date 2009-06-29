/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;

/**
 * @author ray
 */
public class WaitSncCommand extends AbstractToggleCommand
{
    /**
     * @param agent
     */
    public WaitSncCommand(Agent agent)
    {
        super(agent);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.commands.AbstractToggleCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length == 1)
        {
            final boolean v = getAgent().getProperties().get(SoarProperties.WAITSNC);
            return "Current waitsnc setting: --" + (v ? "--enabled" : "--disabled"); 
        }
        return super.execute(args);
    }

    /* (non-Javadoc)
     * @see org.jsoar.tcl.AbstractToggleCommand#execute(org.jsoar.kernel.Agent, boolean)
     */
    @Override
    protected void execute(Agent agent, boolean enable) throws SoarException
    {
        agent.getProperties().set(SoarProperties.WAITSNC, enable);
    }
}
