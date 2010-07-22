/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public class WarningsCommand extends AbstractToggleCommand
{

    /**
     * @param agent
     */
    public WarningsCommand(Agent agent)
    {
        super(agent);
    }

    /* (non-Javadoc)
     * @see org.jsoar.tcl.AbstractToggleCommand#execute(org.jsoar.kernel.Agent, boolean)
     */
    @Override
    protected void execute(Agent agent, boolean enable) throws SoarException
    {
        agent.getPrinter().setPrintWarnings(enable);
    }
    
    @Override
    protected boolean query(Agent agent)
    {
        return agent.getPrinter().isPrintWarnings();
    }
}
