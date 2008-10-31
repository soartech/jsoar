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
public class StructurePreferencesCommand
{
    public static class Result
    {
        private final List<ResultEntry> entries;
        private final List<Wme> impasseWmes;
        private final List<Wme> ioWmes;
        
        /**
         * @param entries
         * @param impasseWmes
         * @param ioWmes
         */
        public Result(List<ResultEntry> entries, List<Wme> impasseWmes, List<Wme> ioWmes)
        {
            this.entries = entries;
            this.impasseWmes = impasseWmes;
            this.ioWmes = ioWmes;
        }

        /**
         * @return the entries
         */
        public List<ResultEntry> getEntries()
        {
            return entries;
        }

        /**
         * @return the impasseWmes
         */
        public List<Wme> getImpasseWmes()
        {
            return impasseWmes;
        }

        /**
         * @return the ioWmes
         */
        public List<Wme> getIoWmes()
        {
            return ioWmes;
        }
        
        
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
        ResultEntry(Preference pref, boolean osupported, String source, List<Wme> sourceWmes)
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
    
    public Result getPreferencesForObject(Identifier idIn)
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
        
        return new Result(entries, impasseWmes, ioWmes);
    }
    
    public Result getPreferences(Agent agent, Identifier idIn, Symbol attr)
    {
        return null;
    }
    
    private ResultEntry createEntry(Preference pref)
    {
        String source = pref.inst.prod != null ? pref.inst.prod.getName().toString() : "[dummy production]";
        List<Wme> sourceWmes = pref.inst.getBacktraceWmes();
        
        return new ResultEntry(pref, pref.o_supported, source, sourceWmes);
    }
}
