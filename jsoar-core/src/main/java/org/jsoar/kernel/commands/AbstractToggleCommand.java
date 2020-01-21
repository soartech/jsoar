/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.collect.Lists;

/**
 * Base class for handling "toggle" commands, i.e. commands with options like
 * {@code [--on|--off|-e|-d|--enable|--disable]}
 * 
 * @author ray
 */
abstract class AbstractToggleCommand implements SoarCommand
{
    private enum Options
    {
        disable, enable, off, On
    }

    private static final String ERROR_MESSAGE = "Option must be one of [--on|--off|-e|-d|--enable|--disable]";

    private final OptionProcessor<Options> options = OptionProcessor.create();

    private final Agent agent;

    AbstractToggleCommand(Agent agent)
    {
        this.agent = agent;
        
        for (Options o : Options.values()) {
            options.newOption(o).done();
        }
    }

    /**
     * @return the agent
     */
    public Agent getAgent()
    {
        return agent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        List<String> nonOpts = options.process(Lists.newArrayList(args));

        if (!nonOpts.isEmpty())
            throw new SoarException(ERROR_MESSAGE);

        if (options.has(Options.enable) || options.has(Options.On))
            execute(agent, true);
        else if (options.has(Options.disable) || options.has(Options.off))
            execute(agent, false);
        else
            return "The current " + args[0] + " setting is: "
                    + (query(agent) ? "enabled" : "disabled");

        return "";
    }

    protected abstract boolean query(Agent agent);

    protected abstract void execute(Agent agent, boolean enable)
            throws SoarException;
}