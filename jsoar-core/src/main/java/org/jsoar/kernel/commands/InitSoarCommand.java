/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public final class InitSoarCommand implements SoarCommand
{
    private final Agent agent;

    public InitSoarCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length != 1)
        {
            throw new SoarException(String.format("%s takes no arguments", args[0]));
        }
        agent.initialize();
        agent.getPrinter().startNewLine().print("Agent reinitialized\n").flush();
        return "";
    }
}