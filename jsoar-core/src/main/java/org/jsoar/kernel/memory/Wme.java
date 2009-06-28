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
import org.jsoar.util.adaptables.Adaptable;

/**
 * Public interface for a working memory element for use by I/O code.
 * 
 * @author ray
 * @see Wmes
 * @see InputWme
 */
public interface Wme extends Formattable, Adaptable
{
    /**
     * @return The identifier field of the WME. This value will never change.
     */
    Identifier getIdentifier();
    
    /**
     * @return The attribute field of the WME. This value will never change.
     */
    Symbol getAttribute();
    
    /**
     * @return The value field of the WME. This value will never change.
     */
    Symbol getValue();
    
    /**
     * @return the WME's time tag. This value will never change.
     */
    int getTimetag();
    
    /**
     * @return true iff this WME is an acceptable preference WME. This value
     *      will never change.
     */
    boolean isAcceptable();
    
    /**
     * @return An iterator over all the children of this WME, if any. If the 
     *  value of the WME is not an identifier, returns an empty iterator.
     */
    Iterator<Wme> getChildren();
    
    /**
     * @return The preferences supporting this WME. Returns an empty iterator 
     *      for architecture or I/O WMEs.
     */
    Iterator<Preference> getPreferences();
}
