/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 27, 2010
 */
package org.jsoar.kernel.smem;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.Cache;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.Optimization;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

/**
 * @author ray
 */
class DefaultSemanticMemoryCommand implements SoarCommand
{
    private final Adaptable context;
    private final DefaultSemanticMemory smem;
    
    public DefaultSemanticMemoryCommand(Adaptable context)
    {
        this.context = context;
        this.smem = Adaptables.require(getClass(), context, DefaultSemanticMemory.class);
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
        if(key == null)
        {
            throw new SoarException("Unknown parameter '" + name + "'");
        }
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
        final PropertyManager props = smem.getParams().getProperties();
        if(name.equals("learning"))
        {
            props.set(DefaultSemanticMemoryParams.LEARNING, "on".equals(value));
        }
        else if(smem.getDatabase() != null)
        {
            // TODO: This check should be done in the property system
            throw new SoarException("This parameter is protected while the semantic memory database is open");
        }
        else if(name.equals("driver"))
        {
            props.set(DefaultSemanticMemoryParams.DRIVER, value);
        }
        else if(name.equals("protocol"))
        {
            props.set(DefaultSemanticMemoryParams.PROTOCOL, value);
        }
        else if(name.equals("path"))
        {
            props.set(DefaultSemanticMemoryParams.PATH, value);
        }
        else if(name.equals("lazy-commit"))
        {
            props.set(DefaultSemanticMemoryParams.LAZY_COMMIT, "on".equals(value));
        }
        else if(name.equals("cache"))
        {
            props.set(DefaultSemanticMemoryParams.CACHE, Cache.valueOf(value));
        }
        else if(name.equals("optimization"))
        {
            props.set(DefaultSemanticMemoryParams.OPTIMIZATION, Optimization.valueOf(value));
        }
        else if(name.equals("thresh"))
        {
            props.set(DefaultSemanticMemoryParams.THRESH, Long.valueOf(value));
        }
        else
        {
            throw new SoarException("Unknown parameter '" + name + "'");
        }
        
        return "";
    }

    private String doInit(int i, String[] args) throws SoarException
    {
        // Because of LTIs, re-initializing requires all other memories to be reinitialized.        
        
        // epmem - close before working/production memories to get re-init benefits
        // TODO EPMEM this->DoCommandInternal( "epmem --close" );
        
        // smem - close before working/production memories to prevent id counter mess-ups
        smem.smem_close();

        // production memory (automatic init-soar clears working memory as a result) 
        //this->DoCommandInternal( "excise --all" );
        
        // Excise all just removes all rules and does init-soar
        final Agent agent = Adaptables.require(getClass(), context, Agent.class);
        for(Production p : new ArrayList<Production>(agent.getProductions().getProductions(null)))
        {
            agent.getProductions().exciseProduction(p, false);
        }
        agent.initialize();
        
        return "";
    }

    private String doStats(int i, String[] args) throws SoarException
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        final DefaultSemanticMemoryStats p = smem.getStats();
        if(args.length == i + 1)
        {
            pw.printf("Memory Usage: %d%n", p.mem_usage.get());
            pw.printf("Memory Highwater: %d%n", p.mem_high.get());
            pw.printf("Retrieves: %d%n", p.retrieves.get());
            pw.printf("Queries: %d%n", p.queries.get());
            pw.printf("Stores: %d%n", p.stores.get());
            pw.printf("Nodes: %d%n", p.nodes.get());
            pw.printf("Edges: %d%n", p.edges.get());
        }
        else
        {
            final String name = args[i+1];
            final PropertyKey<?> key = DefaultSemanticMemoryStats.getProperty(smem.getParams().getProperties(), name);
            if(key == null)
            {
                throw new SoarException("Unknown stat '" + name + "'");
            }
            pw.printf("%s%n", smem.getParams().getProperties().get(key).toString());
        }
        
        pw.flush();
        return sw.toString();
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
        pw.printf("protocol: %s%n", p.protocol);
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
