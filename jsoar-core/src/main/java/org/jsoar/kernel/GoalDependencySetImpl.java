/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.util.Arguments;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * <p>The Goal Dependency Set is a data structure used in Operand2 to maintain
 * the integrity of a subgoal with respect to changes in supergoal WMEs.
 * Whenever a WME in the goal's dependency set changes, the goal is immediately
 * removed.  The routines for maintaining the GDS and determining if a goal
 * should be retracted are in decide.c
 *
 * <p>Fields in a goal dependency set:
 * <ul>
 *    <li><b>goal:</b>  points to the goal for which this dependency set was created.
 *           The goal also has a pointer back to the GDS.
 *
 *    <li><b>wmes_in_gds:</b>  A DLL of WMEs in the goal dependency set
 * </ul>
 * 
 * <p>The GDS is created only when necessary; that is, when an o-supported WME
 * is created in some subgoal and that subgoal has no GDS already.  The
 * instantiations that led to the creation of the o-supported WME are 
 * examined; any supergoal WMEs in these instantiations are added to the 
 * wmes_in_gds DLL.  The GDS for each goal is examined for every WM change;
 * if a WME changes that is on a GDS, the goal that the GDS points to is
 * immediately removed.  
 *
 * <p>When a goal is removed, the GDS is not immediately removed.  Instead,
 * whenever a WME is removed (or when it is added to another GDS), we check
 * to also make certain that its GDS has other WMEs on the wmes_in_gds DLL.
 * If not, then we remove the GDS then.  This delay avoids having to scan
 * over all the WMEs in the GDS in addition to removing the goal (i.e., the
 * maintenance cost is amortized over a number of WM phases).
 * 
 * <p>gdatastructs.h:71:goal_dependency_set
 * 
 * @author ray
 */
public class GoalDependencySetImpl implements GoalDependencySet
{
    /**
     * pointer to the goal for the dependency set
     */
    private IdentifierImpl goal;
    
    /**
     * pointer to the dll of WMEs in GDS of goal
     * 
     * <p>gdatastructs.h:71:wmes_in_gds
     */
    private WmeImpl wmes;
    
    public GoalDependencySetImpl(IdentifierImpl goal)
    {
        Arguments.checkNotNull(goal, "goal");
        this.goal = goal;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.GoalDependencySet#getGoal()
     */
    public IdentifierImpl getGoal()
    {
        return goal;
    }
    
    public void clearGoal()
    {
        if(this.goal == null)
        {
            throw new IllegalStateException("GDS goal has already been cleared");
        }
        this.goal = null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.GoalDependencySet#getWmes()
     */
    public Iterator<Wme> getWmes()
    {
        return new WmeIterator(wmes);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.GoalDependencySet#isEmpty()
     */
    public boolean isEmpty()
    {
        return wmes == null;
    }
    
    /**
     * Add a WME to the head of this GDS's WME list
     * 
     * <p>decide.cpp:2562:add_wme_to_gds
     * 
     * @param w The wme
     */
    public void addWme(WmeImpl w)
    {
        // Set the correct GDS for this wme (wme's point to their gds)
        w.gds = this;
        
        w.gds_next = wmes;
        w.gds_prev = null;
        if(wmes != null)
        {
            wmes.gds_prev = w;
        }
        wmes = w;
    }
    
    /**
     * Remove a WME from the list of WMEs in this GDS
     * 
     * <p>decide.cpp:elaborate_gds
     * <p>wmem.cpp:135:remove_wme_from_wm
     * 
     * @param w The wme to remove
     * @throws IllegalArgumentException if w is not part of this GDS
     */
    public void removeWme(WmeImpl w)
    {
        if(w.gds != this)
        {
            throw new IllegalArgumentException(String.format("%s is not a member of GDS %s", w, goal));
        }
        
        if(w.gds_next != null)
        {
            w.gds_next.gds_prev = w.gds_prev;
        }
        if(w.gds_prev != null)
        {
            w.gds_prev.gds_next = w.gds_next;
        }
        else
        {
            wmes = w.gds_next;
        }
        w.gds_next = null;
        w.gds_prev = null;
        
        // We have to check for GDS removal anytime we take a
        // WME off the GDS wme list, not just when a WME is
        // removed from memory.
        // TODO: Is this even necessary in JSoar??
        if(wmes == null)
        {
            w.gds = null;
        }
    }
    
    private static class WmeIterator implements Iterator<Wme>
    {
        private WmeImpl current;
        
        public WmeIterator(WmeImpl current)
        {
            this.current = current;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext()
        {
            return current != null;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Override
        public Wme next()
        {
            if(current == null)
            {
                throw new NoSuchElementException("At end of GDS WME list");
            }
            final Wme value = current;
            current = current.gds_next;
            return value;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Cannot remove WMEs from GDS with iterator");
        }
        
    }
}
