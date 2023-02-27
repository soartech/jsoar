/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 8, 2009
 */
package org.jsoar.kernel.rete;

import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.MatchSetEntry;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;

/**
 * @author ray
 */
public class DefaultMatchSetEntry implements MatchSetEntry
{
    private final Production production;
    private final EntryType type;
    private final List<Wme> wmes;
    
    public DefaultMatchSetEntry(Production production, EntryType type,
            List<Wme> wmes)
    {
        this.production = production;
        this.type = type;
        this.wmes = Collections.unmodifiableList(wmes);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.MatchSetEntry#getProduction()
     */
    @Override
    public Production getProduction()
    {
        return production;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.MatchSetEntry#getWmes()
     */
    @Override
    public List<Wme> getWmes()
    {
        return wmes;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.MatchSetEntry#getType()
     */
    @Override
    public EntryType getType()
    {
        return type;
    }
    
}
