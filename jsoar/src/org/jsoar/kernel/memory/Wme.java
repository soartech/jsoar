/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Formattable;
import java.util.Iterator;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Public interface for a working memory element for use by I/O code.
 * 
 * @author ray
 */
public interface Wme extends Formattable
{
    /**
     * @return The identifier field of the WME
     */
    Identifier getIdentifier();
    
    /**
     * @return The attribute field of the WME
     */
    Symbol getAttribute();
    
    /**
     * @return The value field of the WME
     */
    Symbol getValue();
    
    /**
     * @return the WME's time tag
     */
    int getTimetag();
    
    /**
     * @return An iterator over all the children of this WME, if any. If the 
     *  value of the WME is not an identifier, returns an empty iterator.
     */
    Iterator<Wme> getChildren();
}
