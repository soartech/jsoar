/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Command that prints out all registered RHS functions
 * 
 * @author ray
 */
public final class RhsFunctionsCommand implements SoarCommand
{
    private final Agent agent;

    public RhsFunctionsCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        final Printer p = agent.getPrinter();
        
        p.startNewLine();
        
        final List<RhsFunctionHandler> handlers = agent.getRhsFunctions().getHandlers();
        Collections.sort(handlers, new Comparator<RhsFunctionHandler>(){

            @Override
            public int compare(RhsFunctionHandler a, RhsFunctionHandler b)
            {
                return a.getName().compareTo(b.getName());
            }});
        
        for(RhsFunctionHandler f : handlers)
        {
            int max = f.getMaxArguments();
            p.print("%20s (%d, %s)%n", f.getName(), f.getMinArguments(), max == Integer.MAX_VALUE ? "*" : Integer.toString(max));
        }
        return "";
    }
}