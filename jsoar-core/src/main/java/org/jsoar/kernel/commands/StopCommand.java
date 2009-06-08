/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommand;

/**
 * http://winter.eecs.umich.edu/soarwiki/Run
 * 
 * <p>Simple implementation of stop-soar command. Must be manually installed.
 * 
 * @author ray
 */
public final class StopCommand implements SoarCommand
{
    private final ThreadedAgent threadedAgent;
    
    public StopCommand(ThreadedAgent threadedAgent)
    {
        this.threadedAgent = threadedAgent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        threadedAgent.stop();
        return "";
    }
}