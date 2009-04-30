/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.Agent;
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
     * Begin constructing a new WME matcher. This uses a builder pattern. 
     * Chain methods together to construct a predicate the WME you'd like to
     * find.
     * 
     * <p>For example, to find a WME on the output link with the attribute
     * "my-command":
     * 
     * <pre>{@code
     * Wme w = Wmes.matcher(agent).attr("my-command").find(agent.getInputOutput().getOutputLink());
     * }</pre>
     * 
     * @param syms the agent's symbol factory
     * @return a matcher builder
     */
    public static MatcherBuilder matcher(SymbolFactory syms)
    {
        return new MatcherBuilder(syms);
    }
    
    /**
     * Convenience version of {@link #matcher(SymbolFactory)}.
     * 
     * @param agent the agent
     * @return new matcher builder
     * @see #matcher(SymbolFactory)
     */
    public static MatcherBuilder matcher(Agent agent)
    {
        return matcher(agent.getSymbols());
    }
    
    /**
     * Create a new predicate that matches a particular id/attr/value pattern for
     * a wme. The returned predicate can be used in the filter methods of the
     * Google collections API.
     * 
     * <p><b>Note</b>: It is generally preferable to use {@link #matcher(Agent)} to
     * build a match predicate rather than using this method directly.
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
    
    /**
     * Filter the given iterator of WMEs with the given predicate.
     * 
     * @param it the WME iterator
     * @param pred predicate that tests WMEs
     * @return list of all WMEs {@code w} for whom {@code pred.apply(w)} is
     *      true
     */
    public static List<Wme> filter(Iterator<Wme> it, Predicate<Wme> pred)
    {
        List<Wme> result = new ArrayList<Wme>();
        for(; it.hasNext();)
        {
            final Wme w = it.next();
            if(pred.apply(w))
            {
                result.add(w);
            }
        }
        return result;
    }
    
    public static class MatcherBuilder
    {
        private final SymbolFactory syms;
        private Identifier id;
        private Object attr;
        private Object value;
        private int timetag = -1;
        
        private MatcherBuilder(SymbolFactory syms)
        {
            this.syms = syms;
        }
        
        public MatcherBuilder reset()
        {
            this.id = null;
            this.attr = null;
            this.value = null;
            this.timetag = -1;
            return this;
        }
        
        /**
         * @param id the desired id, or <code>null</code> for don't care
         * @return this
         */
        public MatcherBuilder id(Identifier id)
        {
            this.id = id;
            return this;
        }
        
        /**
         * @param attr the desired attribute, or <code>null</code> for don't care
         * @return
         */
        public MatcherBuilder attr(Object attr)
        {
            this.attr = attr;
            return this;
        }
        
        /**
         * @param value the desired value, or <code>null</code> for don't care
         * @return
         */
        public MatcherBuilder value(Object value)
        {
            this.value = value;
            return this;
        }
        
        /**
         * @param timetag the desired timetag, or <code>-1</code> for don't care
         * @return
         */
        public MatcherBuilder timetag(int timetag)
        {
            this.timetag = timetag;
            return this;
        }
        
        /**
         * Find a WME in the given iterator
         * 
         * @param it the iterator to search
         * @return the WME, or <code>null</code> if not found
         */
        public Wme find(Iterator<Wme> it)
        {
            return Wmes.find(it, createPredicate());
        }
        
        /**
         * Find a WME in the set of WMEs with the given id, i.e. using
         * {@link Identifier#getWmes()}
         * 
         * @param id the id
         * @return the WME, or <code>null</code> if not found
         */
        public Wme find(Identifier id)
        {
            return find(id.getWmes());
        }
        
        public Wme find(Wme parent)
        {
            return find(parent.getChildren());
        }
        
        /**
         * Find a WME in the given collection of WMEs
         * 
         * @param wmes the wmes
         * @return the WME, or <code>null</code> if not found
         */
        public Wme find(Collection<Wme> wmes)
        {
            return find(wmes.iterator());
        }
        
        public List<Wme> filter(Iterator<Wme> it)
        {
            return Wmes.filter(it, createPredicate());
        }
        
        public List<Wme> filter(Identifier id)
        {
            return filter(id.getWmes());
        }
        
        public List<Wme> filter(Wme parent)
        {
            return filter(parent.getChildren());
        }
        
        public List<Wme> filter(Collection<Wme> wmes)
        {
            return filter(wmes.iterator());
        }
        
        /**
         * @return a predicate for the current state of this builder
         */
        public Predicate<Wme> createPredicate()
        {
            return newMatcher(syms, id, attr, value, timetag);
        }
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
