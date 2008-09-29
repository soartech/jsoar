/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 28, 2008
 */
package org.jsoar.kernel.learning;

import java.util.LinkedList;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.lhs.Condition;

/**
 * @author ray
 */
public class Explain
{
    private final Agent context;
    
    /**
     * <p>gsysparam.h:140:EXPLAIN_SYSPARAM
     * <p>Defaults to false in init_soar()
     */
    private boolean enabled = false;
    
    /**
     * @param agent
     */
    public Explain(Agent agent)
    {
        this.context = agent;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled)
    {
        // TODO There's probably more to do here (EXPLAIN_SYSPARAM on)
        this.enabled = enabled;
    }

    /**
     * 
     */
    public void reset_backtrace_list()
    {
        // TODO Implement reset_backtrace_list
        throw new UnsupportedOperationException("reset_backtrace_list is not implemented");
        
    }

    /**
     * @param temp_explain_chunk
     */
    public void explain_add_temp_to_chunk_list(ExplainChunk temp_explain_chunk)
    {
        // TODO Implement explain_add_temp_to_chunk_list
        throw new UnsupportedOperationException("explain_add_temp_to_chunk_list is not implemented");
    }

    /**
     * @param temp_explain_backtrace
     * @param grounds_to_print
     * @param pots_to_print
     * @param locals_to_print
     * @param negateds_to_print
     */
    public void explain_add_temp_to_backtrace_list(Backtrace temp_explain_backtrace,
            LinkedList<Condition> grounds_to_print, LinkedList<Condition> pots_to_print,
            LinkedList<Condition> locals_to_print, LinkedList<Condition> negateds_to_print)
    {
        // TODO Implement explain_add_temp_to_backtrace_list
        throw new UnsupportedOperationException("explain_add_temp_to_backtrace_list is not implemented");
    }

    
}
