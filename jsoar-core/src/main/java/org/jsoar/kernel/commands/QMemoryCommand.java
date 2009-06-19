/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.io.quick.DefaultQMemory;
import org.jsoar.kernel.io.quick.QMemory;
import org.jsoar.kernel.io.quick.SoarQMemoryAdapter;
import org.jsoar.util.commands.SoarCommand;

/**
 * Manipulate quick memory from command-line
 * 
 * <p>Usage:
 * 
 * <pre>{@code
 * 
 * Get the value of a qmemory path:
 * > qmemory --get <path>
 * 
 * Set the value of a qmemory path (use bars to force string):
 * > qmemory --set <path> <value>
 * 
 * Remove a qmemory path:
 * > qmemory --remove <path>
 * 
 * Clear all qmemory:
 * > qmemory --clear
 * 
 * }</pre>
 * 
 * @author ray
 */
public final class QMemoryCommand implements SoarCommand
{
    private final SoarQMemoryAdapter adapter;

    public QMemoryCommand(Agent agent)
    {
        // TODO: I think this should probably be created by the agent itself or passed
        // in to the command. This will be ok for now.
        this.adapter = SoarQMemoryAdapter.attach(agent, DefaultQMemory.create());
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length < 2)
        {
            throw new SoarException("Expected one of --get, --set, or --remove");
        }
        
        final String arg = args[1];
        if("-g".equals(arg) || "--get".equals(arg))
        {
            return doGet(args, 2);
        }
        else if("-s".equals(arg) || "--set".equals(arg))
        {
            return doSet(args, 2);
        }
        else if("-r".equals(arg) || "--remove".equals(arg))
        {
            return doRemove(args, 2);
        }
        else if("-c".equals(arg) || "--clear".equals(arg))
        {
            return doClear(args, 2);
        }
        else if(arg.startsWith("-"))
        {
            throw new SoarException("Unsupported option " + arg);
        }
        else
        {
            return doGet(args, 1);
        }
    }

    private String fixPath(String path)
    {
        return path.replace('(', '[').replace(')', ']');
    }
    
    private String doClear(String[] args, int i) throws SoarException
    {
        this.adapter.setSource(DefaultQMemory.create());
        return "";
    }

    private String doRemove(String[] args, int i) throws SoarException
    {
        if(i >= args.length)
        {
            throw new SoarException("Expected <input path>");
        }
        adapter.getSource().remove(fixPath(args[i]));
        return "";
    }

    private String doSet(String[] args, int i) throws SoarException
    {
        if(i + 1 >= args.length)
        {
            throw new SoarException("Expected <input path> <value>");
        }
        
        final QMemory qmemory = adapter.getSource();
        final String path = fixPath(args[i]);
        final String value = args[i+1];
        try
        {
            qmemory.setInteger(path, Integer.parseInt(value));
        }
        catch(NumberFormatException e)
        {
            try
            {
                qmemory.setDouble(path, Double.parseDouble(value));
            }
            catch(NumberFormatException e1)
            {
                if(value.length() >= 2 && value.charAt(0) == '|' && value.charAt(value.length() - 1) == '|')
                {
                    qmemory.setString(path, value.substring(1, value.length() - 1));
                }
                else
                {
                    qmemory.setString(path, value);
                }
            }
        }
        
        return value;
    }

    private String doGet(String[] args, int i) throws SoarException
    {
        if(i >= args.length)
        {
            throw new SoarException("Expected <input path>");
        }
        return adapter.getSource().getString(fixPath(args[i]));
    }
}