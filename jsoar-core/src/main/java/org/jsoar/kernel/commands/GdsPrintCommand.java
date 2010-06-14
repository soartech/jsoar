/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;

/**
 * http://code.google.com/p/soar/wiki/CommandLineInterface#gds-print
 * 
 * <p>Implementation of print-gds command.
 * 
 * @author marinier
 */
public final class GdsPrintCommand implements SoarCommand
{
    private final Agent agent;
    
    public GdsPrintCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        String result = "********************* Current GDS **************************\n"
                      + "stepping thru all wmes in rete, looking for any that are in a gds...\n";
        
        // the command outputs two lists of wmes:
        //  the wme-only list, which is in bottom-to-top order by goal
        //  the goal list, which is in top-to-bottom order by goal
        
        final List<Identifier> goalsTopToBottom = agent.getGoalStack();
        final List<Identifier> goalsBottomtoTop = new ArrayList<Identifier>(goalsTopToBottom);
        Collections.reverse(goalsBottomtoTop);
        
        // list wmes from goals in bottom-to-top order
        for(final Identifier goal : goalsBottomtoTop)
        {
            final GoalDependencySet gds = Adaptables.adapt(goal, GoalDependencySet.class);
            if(gds == null)
            {
                continue;
            }
            
            final Iterator<Wme> itr = gds.getWmes();
            while(itr.hasNext())
            {
                final Wme w = itr.next();
                result += "  For Goal  " + goal.toString() + "  " + w.toString() + "\n";
            }
        }
        
        result += "************************************************************\n";
        
        // list goals with wmes in top-to-bottom order
        for(final Identifier goal : goalsTopToBottom)
        {
            result += "  For Goal  " + goal.toString();
            final GoalDependencySet gds = Adaptables.adapt(goal, GoalDependencySet.class);
            if(gds == null)
            {
                result += "  : No GDS for this goal.\n";
                continue;
            }
            
            result += "\n";
            
            final Iterator<Wme> itr = gds.getWmes();
            while(itr.hasNext())
            {
                final Wme w = itr.next();
                result += "                " + w.toString() + "\n";
            }
        }
        
        result += "************************************************************\n";
        
        return result;
    }
}