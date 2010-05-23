package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.timing.ExecutionTimers;

public class TimersCommand extends AbstractToggleCommand {

    /**
     * @param agent
     */
    public TimersCommand()
    {
        super(null);
    }

    /* (non-Javadoc)
     * @see org.jsoar.tcl.AbstractToggleCommand#execute(org.jsoar.kernel.Agent, boolean)
     */
    @Override
    protected void execute(Agent agent, boolean enable) throws SoarException
    {
        ExecutionTimers.setEnabled(enable);
    }

}
