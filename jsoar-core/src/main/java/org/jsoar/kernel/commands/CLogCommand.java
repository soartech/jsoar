/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.TeeWriter;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * @author ray
 */
public final class CLogCommand implements SoarCommand
{
    private static final List<String> offOptions = Arrays.asList("-c", "--close", "-o", "--off", "-d", "--disable");
    private static final List<String> queryOptions = Arrays.asList("-q", "--query");
    
    private final Agent agent;
    private LinkedList<Writer> writerStack = new LinkedList<Writer>();

    public CLogCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        if(args.length == 2)
        {
            String arg = args[1];
            if(offOptions.contains(arg))
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
            else if(queryOptions.contains(arg))
            {
                return writerStack.isEmpty() ? "closed" : String.format("open (%d writers)", writerStack.size());
            }
            else if(arg.equals("stdout"))
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
        // TODO: Implement -a, --add, -A, --append, -e, --existing
        
        throw new SoarException("Expected 1 argument, got " + (args.length - 1));
    }
}