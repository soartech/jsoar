/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 29, 2008
 */
package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
public class StructuredPreferencesCommand
{
    public static class Result
    {
        private final String error;
        private final Identifier queryId;
        private final Symbol queryAttr;
        private final Symbol queryValue;
        
        private final List<ResultEntry> entries;
        private final List<Wme> impasseWmes;
        private final List<Wme> ioWmes;
        
        private Result(Identifier queryId, Symbol queryAttr, Symbol queryValue, 
                     List<ResultEntry> entries, List<Wme> impasseWmes, List<Wme> ioWmes)
        {
            this.error = null;
            this.queryId = queryId;
            this.queryAttr = queryAttr;
            this.queryValue = queryValue;
            this.entries = entries;
            this.impasseWmes = impasseWmes;
            this.ioWmes = ioWmes;
        }
        
        private Result(String error)
        {
            this.error = error;
            this.queryId = null;
            this.queryAttr = null;
            this.queryValue = null;
            this.entries = null;
            this.impasseWmes = null;
            this.ioWmes = null;
        }
        
        public String getError() { return error; }
        
        public Identifier getQueryId() { return queryId; }
        public Symbol getQueryAttribute() { return queryAttr; }
        public Symbol getQueryValue() { return queryValue; }

        public List<ResultEntry> getEntries() { return entries; }
        public List<Wme> getImpasseWmes() { return impasseWmes; }
        public List<Wme> getIoWmes() { return ioWmes; }
    }
    
    public static class ResultEntry
    {
        private final Preference pref;
        private final boolean osupported;
        private final String source;
        private final List<Wme> sourceWmes;
        
        /**
         * @param pref
         * @param osupported
         * @param source
         * @param sourceWmes
         */
        private ResultEntry(Preference pref, boolean osupported, String source, List<Wme> sourceWmes)
        {
            this.pref = pref;
            this.osupported = osupported;
            this.source = source;
            this.sourceWmes = Collections.unmodifiableList(sourceWmes);
        }
        
        public PreferenceType getType() { return pref.type; }
        public Identifier getIdentifier() { return pref.id; }
        public Symbol getAttribute() { return pref.attr; }
        public Symbol getValue() { return pref.value; }
        public Symbol getReferent() { return pref.referent; }
        public boolean isOSupported() { return osupported; }
        public String getSource() { return source; }
        public List<Wme> getSourceWmes() { return sourceWmes; }
        
    }
    
    /**
     * Returns preferences for all WMEs with the given id and optional attribute.
     * 
     * @param queryId The id. Must be non-<code>null</code>
     * @param queryAttr Optional attribute. If <code>null</code>, then prefs for all
     *      WMES (id ^* *) will be returned.
     * @return Preferences for (id ^attr *)
     */
    public Result getPreferences(Identifier queryId, Symbol queryAttr)
    {
        if(queryId == null)
        {
            throw new IllegalArgumentException("queryId");
        }
        
        if(queryAttr == null)
        {
            return getPreferencesForObject(queryId);
        }
        
        final IdentifierImpl id = (IdentifierImpl) queryId;
        final List<ResultEntry> entries = new ArrayList<ResultEntry>();
        
        Slot s = Slot.find_slot(id, queryAttr);
        if (s == null)
        {
            return new Result(String.format("No slot found for (%s ^%s *)", queryId, queryAttr));
        }
        for (Preference p = s.getAllPreferences(); p != null; p = p.nextOfSlot)
        {
            entries.add(createEntry(p));
        }

        return new Result(queryId, queryAttr, null, entries, new ArrayList<Wme>(), new ArrayList<Wme>());
    }
    
    /**
     * Returns preferences for all WMEs with the given id as a value, i.e. (* ^* id)
     * 
     * @param agent the agent
     * @param valueId The value id
     * @return Preferences for (* ^* id)
     */
    public Result getPreferencesForValue(Agent agent, Identifier valueId)
    {
        if(agent == null)
        {
            throw new IllegalArgumentException("agent");
        }
        if(valueId == null)
        {
            throw new IllegalArgumentException("valueId");
        }
        
        final List<ResultEntry> entries = new ArrayList<ResultEntry>();
        for(WmeImpl w : agent.rete.getAllWmes())
        {
            if(w.getValue() == valueId)
            {
                if (w.preference != null)
                {
                    Slot s = Slot.find_slot(w.id, w.attr);
                    if (s == null)
                    {
                        // TODO
                        // printer.print("    This is an arch-wme and has no prefs.\n");
                    }
                    else
                    {
                        for (Preference p = s.getAllPreferences(); p != null; p = p.nextOfSlot)
                        {
                            if(p.value == valueId)
                            {
                                entries.add(createEntry(p));
                            }
                        }
                    }
                    // print it
                }
                
            }
        }
        return new Result(null, null, valueId, entries, new ArrayList<Wme>(), new ArrayList<Wme>());
    }
    
    private Result getPreferencesForObject(Identifier idIn)
    {
        final IdentifierImpl id = (IdentifierImpl) idIn;
        final List<ResultEntry> entries = new ArrayList<ResultEntry>();
        // step thru dll of slots for ID, printing prefs for each one
        for (Slot s : id.slots)
        {
            for (Preference p = s.getAllPreferences(); p != null; p = p.nextOfSlot)
            {
                entries.add(createEntry(p));
            }
        }
        final List<Wme> impasseWmes = new ArrayList<Wme>();
        for (WmeImpl w = id.getImpasseWmes(); w != null; w = w.next)
        {
            impasseWmes.add(w);
        }
        final List<Wme> ioWmes = new ArrayList<Wme>();
        for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
        {
            ioWmes.add(w);
        }
        
        return new Result(idIn, null, null, entries, impasseWmes, ioWmes);
        
    }
    
    private ResultEntry createEntry(Preference pref)
    {
        String source = pref.inst.prod != null ? pref.inst.prod.getName().toString() : "[dummy production]";
        List<Wme> sourceWmes = pref.inst.getBacktraceWmes();
        
        return new ResultEntry(pref, pref.o_supported, source, sourceWmes);
    }
}
