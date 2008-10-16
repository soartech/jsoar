/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.Iterator;

import org.jsoar.kernel.memory.Wme;

/**
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
}
