/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Trace.Category;

/**
 * Implementation of the "verbose" command.
 * 
 * @author ray
 */
public class VerboseCommand extends AbstractToggleCommand
{

    /**
     * @param agent
     */
    public VerboseCommand(Agent agent)
    {
        super(agent);
    }

    /* (non-Javadoc)
     * @see org.jsoar.tcl.AbstractToggleCommand#execute(org.jsoar.kernel.Agent, boolean)
     */
    @Override
    protected void execute(Agent agent, boolean enable) throws SoarException
    {
        agent.getTrace().setEnabled(Category.VERBOSE, enable);
    }
    
    @Override
    protected boolean query(Agent agent)
    {
        return agent.getTrace().isEnabled(Category.VERBOSE);
    }
    @Override
    public Object getCommand() {
        //todo - when implementing picocli, return the runnable
        return null;
    }
}
