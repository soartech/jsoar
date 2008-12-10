/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.events;

import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Event fired during when new output is available on the input link. The
 * WMEs on the output link may be handled directly through the iterator
 * returned by {@link #getWmes()}, or may process commands through
 * {@link InputOutput#getPendingCommands()}.
 * 
 * @author ray
 */
public class OutputEvent extends AbstractInputOutputEvent
{
    public static enum OutputMode
    {
        ADDED_OUTPUT_COMMAND,
        MODIFIED_OUTPUT_COMMAND,
        REMOVED_OUTPUT_COMMAND
    }
    
    private final OutputMode mode;
    private final List<Wme> wmes;
    
    /**
     * Construct a new event
     * 
     * @param io The I/O interface
     * @param mode The output mode
     * @param wmes List of output WMEs
     */
    public OutputEvent(InputOutput io, OutputMode mode, List<Wme> wmes)
    {
        super(io);
        
        this.mode = mode;
        this.wmes = Collections.unmodifiableList(wmes);
    }

    /**
     * @return the mode
     */
    public OutputMode getMode()
    {
        return mode;
    }

    /**
     * @return the wmes
     */
    public List<Wme> getWmes()
    {
        return wmes;
    }
    
    /**
     * This is a simple utility function for use in users' output functions. 
     * It finds things in an io_wme chain. It takes "outputs" (the io_wme 
     * chain), and "id" and "attr" (symbols to match against the wmes), and 
     * returns the value from the first wme in the chain with a matching id 
     * and attribute. Either "id" or "attr" (or both) can be specified as 
     * "don't care" by giving NULL (0) pointers for them instead of pointers 
     * to symbols. If no matching wme is found, the function returns a NULL 
     * pointer.
     * 
     * <p>io.cpp::get_output_value
     * 
     * @param id Desired id, or <code>null</code> for don't care
     * @param attr Desired attribute, or <code>null</code> for don't care
     * @return value of wme with given id and attribute
     */
    public Symbol getOutputValue(Identifier id, Symbol attr)
    {
        for (Wme iw : wmes)
            if (((id == null) || (id == iw.getIdentifier())) && ((attr == null) || (attr == iw.getAttribute())))
                return iw.getValue();
        return null;
    }

}
