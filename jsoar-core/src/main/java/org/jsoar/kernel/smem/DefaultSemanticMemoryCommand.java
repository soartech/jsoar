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
import org.jsoar.util.properties.PropertyKey;

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
        if(args.length == 1)
        {
            return doSmem();
        }
        
        final String arg = args[1];
        if("-a".equals(arg) || "--add".equals(arg))
        {
            return doAdd(1, args);
        }
        else if("-g".equals(arg) || "--get".equals(arg))
        {
            return doGet(1, args);
        }
        else if("-i".equals(arg) || "--init".equals(arg))
        {
            return doInit(1, args);
        }
        else if("-s".equals(arg) || "--set".equals(arg))
        {
            return doSet(1, args);
        }
        else if("-S".equals(arg) || "--stats".equals(arg))
        {
            return doStats(1, args);
        }
        else if("-t".equals(arg) || "--timers".equals(arg))
        {
            return doTimers(1, args);
        }
        else if("-v".equals(arg) || "--viz".equals(arg))
        {
            return doViz(1, args);
        }
        else if(arg.startsWith("-"))
        {
            throw new SoarException("Unknown option " + arg);
        }
        else
        {
            throw new SoarException("Unknown argument " + arg);
        }
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
    
    private String doGet(int i, String[] args) throws SoarException
    {
        if(i + 1 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String name = args[i+1];
        final PropertyKey<?> key = DefaultSemanticMemoryParams.getProperty(smem.getParams().getProperties(), name);
        
        return smem.getParams().getProperties().get(key).toString();
    }

    private String doSet(int i, String[] args) throws SoarException
    {
        if(i + 2 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String name = args[i+1];
        final String value = args[i+2];
        if(name.equals("learning"))
        {
            smem.getParams().getProperties().set(DefaultSemanticMemoryParams.LEARNING, "on".equals(value));
        }
        
        return null;
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

    private String doSmem()
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        final DefaultSemanticMemoryParams p = smem.getParams();
        pw.printf("SMem learning: %s%n", p.learning.get() ? "on" : "off");
        pw.println();
        pw.println("Storage");
        pw.println("-------");
        pw.printf("driver: %s%n", p.driver);
        pw.printf("path: %s%n", p.path);
        pw.printf("lazy-commit: %s%n", p.lazy_commit.get() ? "on" : "off");
        pw.println();
        pw.println("Performance");
        pw.println("-----------");
        pw.printf("thresh: %d%n", p.thresh.get());
        pw.printf("cache: %s%n", p.cache);
        pw.printf("optimization: %s%n", p.optimization);
        // TODO SMEM timers params
        
        pw.flush();
        return sw.toString();
    }

}
