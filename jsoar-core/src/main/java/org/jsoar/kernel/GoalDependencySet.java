/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 12, 2010
 */
package org.jsoar.kernel;

import java.util.Iterator;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;

/**
 * Public interface for a Goal Dependency Set.
 * 
 * <p>This interface can be retrieved from an {@link Identifier}
 * using adaptables.
 * 
 * @author ray
 * @see Identifier
 */
public interface GoalDependencySet
{
    /**
     * @return the goal for this GDS
     */
    Identifier getGoal();
    
    /**
     * @return an iterator over the list of WMEs in this GDS. This iterator is
     * "live" so if you intend to use it later, make a copy into a list.
     */
    Iterator<Wme> getWmes();
    
    /**
     * @return true if this GDS is empty
     */
    boolean isEmpty();
    
}
