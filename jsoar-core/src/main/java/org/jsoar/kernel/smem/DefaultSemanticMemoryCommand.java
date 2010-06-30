/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 27, 2010
 */
package org.jsoar.kernel.smem;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
class DefaultSemanticMemoryCommand implements SoarCommand
{
    private final DefaultSemanticMemory smem;
    
    public DefaultSemanticMemoryCommand(DefaultSemanticMemory smem)
    {
        this.smem = smem;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(String[] args) throws SoarException
    {
        for(int i = 1; i < args.length; i++)
        {
            final String arg = args[i];
            if("-a".equals(arg) || "--add".equals(arg))
            {
                return doAdd(i, args);
            }
            else if("-g".equals(arg) || "--get".equals(arg))
            {
                return doInit(i, args);
            }
            else if("-i".equals(arg) || "--init".equals(arg))
            {
                return doInit(i, args);
            }
            else if("-s".equals(arg) || "--set".equals(arg))
            {
                return doSmem(i, args);
            }
            else if("-S".equals(arg) || "--stats".equals(arg))
            {
                return doStats(i, args);
            }
            else if("-t".equals(arg) || "--timers".equals(arg))
            {
                return doTimers(i, args);
            }
            else if("-v".equals(arg) || "--viz".equals(arg))
            {
                return doViz(i, args);
            }
            else if(arg.startsWith("-"))
            {
                throw new SoarException("Unknown option " + arg);
            }
            else
            {
               return doSmem(i, args);
            }
        }
        return null;
    }

    private String doAdd(int i, String[] args) throws SoarException
    {
        if(i + 1 == args.length)
        {
            throw new SoarException("No argument for " + args[i] + " option");
        }
        // Braces are stripped by the interpreter, so put them back
        smem.smem_parse_chunks("{" + args[i+1] + "}");
        return "";
    }

    private String doInit(int i, String[] args)
    {
        // TODO Auto-generated method stub
        return null;
    }

    private String doStats(int i, String[] args)
    {
        // TODO Auto-generated method stub
        return null;
    }

    private String doTimers(int i, String[] args)
    {
        // TODO Auto-generated method stub
        return null;
    }

    private String doViz(int i, String[] args) throws SoarException
    {
        if(i + 1 == args.length)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            smem.smem_visualize_store(pw);
            pw.flush();
            return sw.toString();
        }
        // TODO SMEM Commands: --viz with args
        throw new SoarException("smem --viz with args not implemented yet");
    }

    private String doSmem(int i, String[] args)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
