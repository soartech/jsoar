package org.jsoar.tcl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * Command that prints out all property values
 * 
 * @author ray
 */
final class PropertiesCommand implements Command
{
    private final SoarTclInterface ifc;

    /**
     * @param ifc the owning Tcl interface
     */
    PropertiesCommand(SoarTclInterface ifc)
    {
        this.ifc = ifc;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        final Agent agent = ifc.getAgent();
        final Printer p = agent.getPrinter();
        
        p.startNewLine();
        final PropertyManager properties = agent.getProperties();
        final List<PropertyKey<?>> keys = properties.getKeys();
        Collections.sort(keys, new Comparator<PropertyKey<?>>(){

            @Override
            public int compare(PropertyKey<?> a, PropertyKey<?> b)
            {
                return a.getName().compareTo(b.getName());
            }});
        for(PropertyKey<?> key : keys)
        {
            p.print("%30s = %s%s\n", key.getName(), properties.get(key), key.isReadonly() ? " [RO]" : "");
        }
    }
}