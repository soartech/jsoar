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
 * Implementation of the "save-backtraces" command.
 * 
 * @author ray
 */
public class SaveBacktracesCommand extends AbstractToggleCommand
{
    public SaveBacktracesCommand(Agent agent)
    {
        super(agent);
    }

    /* (non-Javadoc)
     * @see org.jsoar.tcl.AbstractToggleCommand#execute(org.jsoar.kernel.Agent, boolean)
     */
    @Override
    protected void execute(Agent agent, boolean enable) throws SoarException
    {
        agent.getProperties().set(SoarProperties.EXPLAIN, enable);
    }
    @Override
    public Object getCommand() {
        //todo - when implementing picocli, return the runnable
        return null;
    }
    @Override
    protected boolean query(Agent agent)
    {
        return agent.getProperties().get(SoarProperties.EXPLAIN);
    }
}
