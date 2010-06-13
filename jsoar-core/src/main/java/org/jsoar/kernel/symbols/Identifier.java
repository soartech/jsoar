/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.EnumSet;
import java.util.Iterator;

import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeType;

/**
 * A Soar identifier symbol.
 * 
 * <p>Instances of this class may be adaptable to:
 * 
 * <ul>
 * <li>{@link GoalDependencySet} if this identifier is a goal and the goal
 *      has a GDS.
 * </ul>
 * 
 * @author ray
 */
public interface Identifier extends Symbol
{
    /**
     * @return The name letter of this id, e.g. in R56, returns 'R'
     */
    char getNameLetter();
    
    /**
     * @return The name number of this id, e.g. in R56, returns 56.
     */
    int getNameNumber();
    
    /**
     * Returns an iterator over all WMEs with this id.
     * 
     * @return Iterator over all WMEs with this id.
     */
    Iterator<Wme> getWmes();
    
    Iterator<Wme> getWmes(EnumSet<WmeType> desired);
    
    /**
     * @return true if this identifier is a goal/state
     */
    boolean isGoal();
}
