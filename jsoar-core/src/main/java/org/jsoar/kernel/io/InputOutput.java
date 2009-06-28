/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.io;

import java.util.List;

import org.jsoar.kernel.events.AsynchronousInputReadyEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.io.beans.SoarBeanOutputManager;
import org.jsoar.kernel.io.quick.SoarQMemoryAdapter;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * This is the JSoar I/O interface. This is the interface you use for handling output
 * commands as well as providing input to the agent.
 * 
 * <p>Note that the methods in this interface should generally only be called 
 * from the {@link InputEvent} and {@link OutputEvent} callbacks in the agent thread. 
 *  
 * <p>See also: <a href="http://code.google.com/p/jsoar/wiki/JSoarInput">JSoar Input Guide</a>
 * and <a href="http://code.google.com/p/jsoar/wiki/JSoarOutput">JSoar Output Guide</a>
 *
 * @see SoarQMemoryAdapter
 * @see Wmes
 * @see InputWmes
 * @see SoarBeanOutputManager
 * 
 * @author ray
 */
public interface InputOutput
{
    /**
     * Returns the symbol factory used to generate I/O component
     * 
     * @return The symbol factory used by this I/O component
     */
    SymbolFactory getSymbols();
    
    /**
     * Create a new input WME. The symbols used must have been created with
     * the symbol factory returned by {@link #getSymbols()}. <b>Use of
     * the utility functions in {@link InputWmes} is encouraged.</b>
     * 
     * <p>io.cpp::add_input_wme
     * 
     * @param id The id of the new WME.
     * @param attr The attribute of the new WME
     * @param value The value of the new WME
     * @return The newly created WME object
     * @throws IllegalArgumentException if any of the ids is <code>null</code>
     */
    InputWme addInputWme(Identifier id, Symbol attr, Symbol value);
            
    /**
     * @return The identifier of the input-link, typically <code>I2</code>
     */
    Identifier getInputLink();
    
    /**
     * @return The identifier of the output-link, typically <code>I3</code>
     */
    Identifier getOutputLink();
    
    /**
     * Returns a list of new output commands added to the output link in the 
     * last decision cycle. The returned list is a copy and may be manipulated
     * by the caller
     * 
     * @return List of new output command WMEs added to the output link in the
     *     last decision cycle.
     */
    List<Wme> getPendingCommands();
    
    /**
     * Notify the agent that asynchronous input is ready. This will ensure that 
     * the agent will break out of suspension (e.g. due to "wait" rhs function)
     * and proceed to the input phase.
     * 
     * <p>This method only causes an {@link AsynchronousInputReadyEvent} to be
     * fired.
     */
    void asynchronousInputReady();
    
}
