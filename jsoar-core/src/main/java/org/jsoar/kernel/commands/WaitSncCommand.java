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
 * Implementation of the "waitsnc" command.
 * 
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
     * @see org.jsoar.tcl.AbstractToggleCommand#execute(org.jsoar.kernel.Agent, boolean)
     */
    @Override
    protected void execute(Agent agent, boolean enable) throws SoarException
    {
        agent.getProperties().set(SoarProperties.WAITSNC, enable);
    }

    @Override
    protected boolean query(Agent agent)
    {
        return getAgent().getProperties().get(SoarProperties.WAITSNC);
    }
}
