/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.util.Arrays;

import org.jsoar.kernel.AgentRunController;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.StringTools;
import org.jsoar.util.commands.SoarCommand;

/**
 * http://winter.eecs.umich.edu/soarwiki/Run
 * 
 * <p>Simple implementation of Run command. Must be manually installed.
 * 
 * @author ray
 */
public final class RunCommand implements SoarCommand
{
    private final AgentRunController controller;
    
    public RunCommand(AgentRunController controller)
    {
        this.controller = controller;
    }
    
    private long getCount(String arg) throws SoarException
    {
        final String countString = arg;
        try
        {
            long n = Long.parseLong(countString);
            if(n < 1)
            {
                throw new SoarException("Expected count larger than 0 for run command, got " + arg);
            }
            return n;
        }
        catch(NumberFormatException e)
        {
            throw new SoarException("Expected integer for run count, got '" + countString + "'");
        }
    }
    
    private RunType getRunType(RunType current, RunType newType, String arg) throws SoarException
    {
        if(current != null)
        {
            throw new SoarException("Multiple run types specified, " + current + " and " + newType);
        }
        
        return newType;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        RunType type = null;
        long count = 0;
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i];
            if("-d".equals(arg) || "--decision".equals(arg))
            {
                type = getRunType(type, RunType.DECISIONS, arg);
            }
            else if("-e".equals(arg) || "--elaboration".equals(arg))
            {
                type = getRunType(type, RunType.ELABORATIONS, arg);
            }
            else if("-p".equals(arg) || "--phase".equals(arg))
            {
                type = getRunType(type, RunType.PHASES, arg);
            }
            else if("-f".equals(arg) || "--forever".equals(arg))
            {
                type = getRunType(type, RunType.FOREVER, arg);
            }
            else if("-o".equals(arg) || "--output".equals(arg))
            {
                type = getRunType(type, RunType.MODIFICATIONS_OF_OUTPUT, arg);
            }
            else if(arg.startsWith("-"))
            {
                throw new SoarException("Unknow option '" + arg + "'");
            }
            else
            {
                long newCount = getCount(arg);
                if(count != 0)
                {
                    throw new SoarException("Multiple counts given for run command: " + StringTools.join(Arrays.asList(args), " "));
                }
                count = newCount;
            }
        }
        if(count == 0)
        {
            count = 1;
        }
        if(type == null)
        {
            type = RunType.FOREVER;
        }
        
        controller.runFor(count, type);
        return "";
    }
}