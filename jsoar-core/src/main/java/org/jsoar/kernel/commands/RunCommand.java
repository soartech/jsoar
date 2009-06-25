/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;
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
    private final ThreadedAgent threadedAgent;
    
    public RunCommand(ThreadedAgent threadedAgent)
    {
        this.threadedAgent = threadedAgent;
    }
    
    private long getCount(int i, String[] args) throws SoarException
    {
        final String arg = args[i];
        if(i + 1 >= args.length)
        {
            throw new SoarException("No count argument for " + arg + " option");
        }
        final String countString = args[i+1].toString();
        try
        {
            long n = Long.parseLong(countString);
            if(n < 1)
            {
                throw new SoarException("Expected count larger than 0 for " + arg + " option");
            }
            return n;
        }
        catch(NumberFormatException e)
        {
            throw new SoarException("Expected integer for run count, got '" + countString + "'");
        }
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        RunType type = RunType.FOREVER;
        long count = 0;
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i];
            if("-d".equals(arg) || "--decision".equals(arg))
            {
                type = RunType.DECISIONS;
                count = getCount(i++, args);
            }
            else if("-e".equals(arg) || "--elaboration".equals(arg))
            {
                type = RunType.ELABORATIONS;
                count = getCount(i++, args);
            }
            else if("-p".equals(arg) || "--phase".equals(arg))
            {
                type = RunType.PHASES;
                count = getCount(i++, args);
            }
            else if("-f".equals(arg) || "--forever".equals(arg))
            {
                type = RunType.FOREVER;
            }
            else
            {
                throw new SoarException("Unknow option '" + arg + "'");
            }
        }
        
        threadedAgent.runFor(count, type);
        return "";
    }
}