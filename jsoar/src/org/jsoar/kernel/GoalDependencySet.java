/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.Arguments;
import org.jsoar.util.ListHead;

/**
 * The Goal Dependency Set is a data strcuture used in Operand2 to maintain
 * the integrity of a subgoal with respect to changes in supergoal WMEs.
 * Whenever a WME in the goal's dependency set changes, the goal is immediately
 * removed.  The routines for maintaining the GDS and determining if a goal
 * should be retracted are in decide.c
 *
 * Fields in a goal dependency set:
 *
 *    goal:  points to the goal for which this dependency set was created.
 *           The goal also has a pointer back to the GDS.
 *
 *    wmes_in_gds:  A DLL of WMEs in the goal dependency set
 *
 * The GDS is created only when necessary; that is, when an o-suppported WME
 * is created in some subgoal and that subgoal has no GDS already.  The
 * instantiations that led to the creation of the o-supported WME are 
 * examined; any supergoal WMEs in these instantiations are added to the 
 * wmes_in_gds DLL.  The GDS for each goal is examined for every WM change;
 * if a WME changes that is on a GDS, the goal that the GDS points to is
 * immediately removed.  
 *
 * When a goal is removed, the GDS is not immediately removed.  Instead,
 * whenever a WME is removed (or when it is added to another GDS), we check
 * to also make certain that its GDS has other WMEs on the wmes_in_gds DLL.
 * If not, then we remove the GDS then.  This delay avoids having to scan
 * over all the WMEs in the GDS in addition to removing the goal (i.e., the
 * maintenance cost is amortized over a number of WM phases).
 * 
 * gdatastructs.h:71:goal_dependency_set
 * 
 * @author ray
 */
public class GoalDependencySet
{
    /**
     * pointer to the goal for the dependency set
     */
    private Identifier goal;
    
    /**
     * pointer to the dll of WMEs in GDS of goal
     */
    public final ListHead<Wme> wmes_in_gds = new ListHead<Wme>();
    
    public GoalDependencySet(Identifier goal)
    {
        Arguments.checkNotNull(goal, "goal");
        this.goal = goal;
    }

    /**
     * @return the goal
     */
    public Identifier getGoal()
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
}
