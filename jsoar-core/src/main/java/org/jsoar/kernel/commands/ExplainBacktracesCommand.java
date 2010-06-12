/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.Explain;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public class ExplainBacktracesCommand implements SoarCommand
{
    private final Agent agent;
    
    public ExplainBacktracesCommand(Agent agent)
    {
        this.agent = agent;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(String[] args) throws SoarException
    {
        final Options options = processArgs(args);
        
        final Explain explain = Adaptables.adapt(agent, Explain.class);
        if(explain == null)
        {
            throw new SoarException("Internal error: Could not find Explain object in agent!");
        }
        if(options.production == null)
        {
            explain.explain_list_chunks();
        }
        else if(options.full)
        {
            explain.explain_trace_named_chunk(options.production);
        }
        else if(options.condition == -1)
        {
            explain.explain_cond_list(options.production);
        }
        else
        {
            explain.explain_chunk(options.production, options.condition);
        }
        return "";
    }

    static Options processArgs(String[] args) throws SoarException
    {
        boolean full = false;
        int condition = -1;
        int i = 1;
        for(; i < args.length; ++i)
        {
            final String arg = args[i];
            if("-f".equals(arg) || "--full".equals(arg))
            {
                full = true;
            }
            else if("-c".equals(arg) || "--condition".equals(arg))
            {
                if(i + 1 == args.length)
                {
                    throw new SoarException("Expected numeric argument for " + arg + " option.");
                }
                try
                {
                    condition = Integer.valueOf(args[i+1]);
                }
                catch(NumberFormatException e)
                {
                    throw new SoarException("Expected numeric argument for " + arg + " option, got " + args[i+1]);
                }
                i++; // skip number
            }
            else if(arg.startsWith("-"))
            {
                throw new SoarException("Unsupported option " + arg + ".");
            }
            else
            {
                break;
            }
        }
        final String production = i < args.length ? args[i] : null;
        
        final Options options = new Options(full, condition, production);
        return options;
    }
    
    static class Options
    {
        public final boolean full;
        public final int condition;
        public final String production;
        
        public Options(boolean full, int condition, String production)
        {
            this.full = full;
            this.condition = condition;
            this.production = production;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + condition;
            result = prime * result + (full ? 1231 : 1237);
            result = prime * result
                    + ((production == null) ? 0 : production.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Options other = (Options) obj;
            if (condition != other.condition)
                return false;
            if (full != other.full)
                return false;
            if (production == null)
            {
                if (other.production != null)
                    return false;
            }
            else if (!production.equals(other.production))
                return false;
            return true;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "Options [condition=" + condition + ", full=" + full
                    + ", production=" + production + "]";
        }
        
        
    }

}
