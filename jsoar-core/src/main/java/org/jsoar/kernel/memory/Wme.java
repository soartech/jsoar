/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Formattable;
import java.util.Iterator;

import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.adaptables.Adaptable;

/**
 * Public interface for a working memory element.
 * 
 * Instances of this class may be adaptable to:
 *
 * * {@link InputWme} if the WME was created on the input-link by environment code, i.e. {@link InputOutput#addInputWme(Identifier, Symbol, Symbol)}
 * * {@link GoalDependencySet} if the WME is part of a GDS.
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
     * When a working memory element is created, Soar assigns it a unique integer `timetag`. The `timetag` are used to
     * distinguish between multiple occurrences of the same WME. As preferences change and elements are added and
     * deleted from working memory, it is possible for a WME to be created, removed, and created again. The second
     * creation of the WME - which bears the same identifier, attribute, and value as the first WME - is different, and
     * therefore is assigned a different `timetag`. This is important because a production will fire only once for a
     * given instantiation, and the instantiation is determined by the `timetag` that match the production and not by
     * the identifier value
     *
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
