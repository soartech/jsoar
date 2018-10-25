/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.TeeWriter;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.collect.Lists;

/**
 * Implementation of the "clog" command.
 * 
 * @author ray
 */
public final class CLogCommand implements SoarCommand
{
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private enum Options
    {
        close, off, disable, query,
        // TODO: Implement -a, --add, -A, --append, -e, --existing
    }
    
    private final Agent agent;
    private LinkedList<Writer> writerStack = new LinkedList<Writer>();

    public CLogCommand(Agent agent)
    {
        this.agent = agent;
        
        options
        .newOption(Options.close)
        .newOption(Options.off)
        .newOption(Options.disable)
        .newOption(Options.query)
        .done();
    }
    @Override
    public Object getCommand() {
        //todo - when implementing picocli, return the runnable
        return null;
    }
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        List<String> nonOpts = options.process(Lists.newArrayList(args));
        
        if (options.has(Options.close) || options.has(Options.disable) || options.has(Options.off))
        {
            if(writerStack.isEmpty())
            {
                throw new SoarException("Log stack is empty");
            }
            final Writer w = writerStack.pop();
            agent.getPrinter().popWriter();
            if(w != null)
            {
                try
                {
                    w.close();
                }
                catch (IOException e)
                {
                    throw new SoarException("While closing writer: " + e.getMessage());
                }
            }
            return "";
        } 
        else if (options.has(Options.query))
        {
            return writerStack.isEmpty() ? "closed" : String.format("open (%d writers)", writerStack.size());
        } 
        else if (nonOpts.size() == 1)
        {
            String arg = nonOpts.get(0);
            
            if(arg.equals("stdout"))
            {
                Writer w = new OutputStreamWriter(System.out);
                writerStack.push(null);
                agent.getPrinter().pushWriter(new TeeWriter(agent.getPrinter().getWriter(), w));
                return "";
            }
            else if(arg.equals("stderr"))
            {
                Writer w = new OutputStreamWriter(System.err);
                writerStack.push(null);
                agent.getPrinter().pushWriter(new TeeWriter(agent.getPrinter().getWriter(), w));
                return "";
            }
            else
            {
                try
                {
                    Writer w = new FileWriter(arg);
                    writerStack.push(w);
                    agent.getPrinter().pushWriter(new TeeWriter(agent.getPrinter().getWriter(), w));
                    return "";
                }
                catch (IOException e)
                {
                    throw new SoarException("Failed to open file '" + arg + "': " + e.getMessage());
                }
            }
        }

        throw new SoarException("Expected 1 argument, got " + (args.length - 1));
    }
}