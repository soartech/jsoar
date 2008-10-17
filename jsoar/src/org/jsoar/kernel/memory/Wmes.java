/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Iterator;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.Arguments;

import com.google.common.base.Predicate;

/**
 * {@link Wme} utility routines
 * 
 * @author ray
 */
public class Wmes
{
    /**
     * Create a new predicate that matches a particular id/attr/value pattern for
     * a wme. The returned predicate can be used in the filter methods of the
     * Google collections API.
     * 
     * @param syms A symbol factory
     * @param id Desired id, or <code>null</code> for any id
     * @param attr Desired attribute, or <code>null</code> for any attribute
     * @param value Desired value, or <code>null</code> for any value
     * @param timetag Desired timetag, or -1 for any timetag
     * @return New predicate object
     */
    public static Predicate<Wme> newMatcher(SymbolFactory syms, Identifier id, Object attr, Object value, int timetag)
    {
        Arguments.checkNotNull(syms, "syms");
        return new MatcherPredicate(id, 
                                   attr != null ? Symbols.create(syms, attr) : null,
                                   value != null ? Symbols.create(syms, value) : null,
                                   timetag);
    }
    
    public static Predicate<Wme> newMatcher(SymbolFactory syms, Identifier id, Object attr, Object value)
    {
        return newMatcher(syms, id, attr, value, -1);
    }
    
    public static Predicate<Wme> newMatcher(SymbolFactory syms, Identifier id, Object attr)
    {
        return newMatcher(syms, id, attr, null);
    }
    
    /**
     * Find the first wme that matches the given predicate
     * 
     * @param it Iterator to search over
     * @param pred The predicate
     * @return The first wme for which predicate is true, or <code>null</code>
     */
    public static Wme find(Iterator<Wme> it, Predicate<Wme> pred)
    {
        for(; it.hasNext();)
        {
            final Wme w = it.next();
            if(pred.apply(w))
            {
                return w;
            }
        }
        return null;
    }
    
    private static class MatcherPredicate implements Predicate<Wme>
    {
        private final Identifier id;
        private final Symbol attr;
        private final Symbol value;
        private final int timetag;
        
        MatcherPredicate(Identifier id, Symbol attr, Symbol value, int timetag)
        {
            this.id = id;
            this.attr = attr;
            this.value = value;
            this.timetag = timetag;
        }

        /* (non-Javadoc)
         * @see com.google.common.base.Predicate#apply(java.lang.Object)
         */
        @Override
        public boolean apply(Wme w)
        {
            if(id != null && id != w.getIdentifier())
            {
                return false;
            }
            if(attr != null && attr != w.getAttribute())
            {
                return false;
            }
            if(value != null && value != w.getValue())
            {
                return false;
            }
            if(timetag >= 0 && timetag != w.getTimetag())
            {
                return false;
            }
            return true;
        }
        
    }
}
