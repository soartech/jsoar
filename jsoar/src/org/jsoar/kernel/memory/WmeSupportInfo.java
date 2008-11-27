/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 26, 2008
 */
package org.jsoar.kernel.memory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.Arguments;

/**
 * Simple class that retrieves support information for a particular WME.
 * 
 * <p>This code is based very loosely on the implementation of the 
 * preferences command in csoar.
 * 
 * @author ray
 */
public class WmeSupportInfo
{
    private final Wme wme;
    private final Preference pref;
    private final boolean osupported;
    private final String valueTrace;
    private final Production source;
    private final List<Wme> sourceWmes;
    
    /**
     * Get support info for the given WME.
     * 
     * @param agent The agent
     * @param wme The wme
     * @return support info  for the given wme, or null if it is architectural
     *      or I/O, i.e. if it has no preference
     * @throws IllegalArgumentException if agent or wme is <code>null</code>.
     */
    public static WmeSupportInfo get(Agent agent, Wme wme)
    {
        Arguments.checkNotNull(agent, "agent");
        Arguments.checkNotNull(wme, "wme");
        
        final Preference pref = wme.getPreference();
        if(pref == null)
        {
            return null;
        }
        
        List<Wme> sourceWmes = pref.inst.getBacktraceWmes();
        
        final String valueTrace;
        if(pref.attr == agent.predefinedSyms.operator_symbol)
        {
            StringWriter w = new StringWriter();
            try
            {
                agent.traceFormats.print_object_trace(w, pref.value);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            valueTrace = w.toString();
        }
        else
        {
            valueTrace = String.format("%s", pref.value);
        }
        return new WmeSupportInfo(wme, valueTrace, pref.o_supported, pref.inst.prod, sourceWmes);
        
    }
    
    /**
     * @param pref
     * @param osupported
     * @param source
     * @param sourceWmes
     */
    private WmeSupportInfo(Wme wme, String valueTrace, boolean osupported, Production source, List<Wme> sourceWmes)
    {
        this.wme = wme;
        this.pref = wme.getPreference();
        this.valueTrace = valueTrace;
        this.osupported = osupported;
        this.source = source;
        this.sourceWmes = Collections.unmodifiableList(sourceWmes);
    }
    
    public PreferenceType getType() { return pref.type; }
    public Identifier getIdentifier() { return pref.id; }
    public Symbol getAttribute() { return pref.attr; }
    public Symbol getValue() { return pref.value; }
    public String getValueTrace() { return valueTrace; }
    public Symbol getReferent() { return pref.referent; }
    public boolean isOSupported() { return osupported; }
    public Production getSource() { return source; }
    public List<Wme> getSourceWmes() { return sourceWmes; }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b.append(String.format("%s is supported by %s:\n", wme, source));
        for(Wme w : getSourceWmes())
        {
            b.append(String.format("   %s", w));
        }
        return b.toString(); 
    }
    
   
    
    
}
