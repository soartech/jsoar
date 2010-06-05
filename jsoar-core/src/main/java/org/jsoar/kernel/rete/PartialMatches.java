/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 5, 2010
 */
package org.jsoar.kernel.rete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.lhs.Condition;

/**
 * Container object for a partial matches of a production. Instances of this
 * object are immutable.
 * 
 * @see Production#getPartialMatches()
 * @see Rete#getPartialMatches(ReteNode)
 * @author ray
 */
public class PartialMatches
{
    /**
     * A match entry
     */
    public static class Entry
    {
        /**
         * The condition
         */
        public final Condition condition;
        
        /**
         * Number of matches
         */
        public final int matches;
        
        /**
         * If the condition is an NCC, this value is non-null and
         * contains match information for the conditions in the NCC. For
         * positive conditions, always {@code null}
         */
        public final List<Entry> negatedSubConditions;
        
        public Entry(Condition condition, int matches,
                List<Entry> negatedSubConditions)
        {
            this.condition = condition;
            this.matches = matches;
            this.negatedSubConditions = negatedSubConditions != null ? 
                    Collections.unmodifiableList(new ArrayList<Entry>(negatedSubConditions)) : null;
        }
        
        public String toString()
        {
            if(negatedSubConditions == null)
            {
                return String.format("%d %s", matches, condition);
            }
            else
            {
                return String.format("-{\n%s\n} %d", negatedSubConditions, matches);
            }
        }
    }
    
    private final List<Entry> entries;
    
    public PartialMatches(List<Entry> entries)
    {
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }
    
    /**
     * @return immutable list of entries
     */
    public List<Entry> getEntries()
    {
        return entries;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return entries.toString();
    }
}
