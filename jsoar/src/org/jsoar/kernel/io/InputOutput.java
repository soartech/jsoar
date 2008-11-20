/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.io;

import java.util.List;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;

/**
 * @author ray
 */
public interface InputOutput
{
    /**
     * @return The symbol factory used by this I/O component
     */
    SymbolFactory getSymbols();
    
    /**
     * Create a new input WME. The symbols used must have been created with
     * the symbol factory returned by {@link #getSymbols()}. Use of
     * the utility functions in {@link Symbols} is encouraged.
     * 
     * <p>io.cpp::add_input_wme
     * 
     * @param id The id of the new WME.
     * @param attr The attribute of the new WME
     * @param value The value of the new WME
     * @return The newly created WME object
     * @throws IllegalArgumentException if any of the ids is <code>null</code>
     */
    Wme addInputWme(Identifier id, Symbol attr, Symbol value);
    
    /**
     * Remove an input WME previously created with {@link #addInputWme(Identifier, Symbol, Symbol)}.
     * 
     * <p>io.cpp:243:remove_input_wme
     * 
     * @param w The WME to remove
     * @throws IllegalArgumentException if the WME is <code>null</code>
     */
    void removeInputWme(Wme w);
    
    /**
     * Update the value of a WME, returning the new WME.  Since WMEs are 
     * immutable, a new WME must be created.
     *  
     * @param w The WME to update
     * @param newValue New value for the WME
     * @return The new WME with id and attr same as input WME and new value
     * @throws IllegalArgumentException if the WME is <code>null</code>
     */
    Wme updateInputWme(Wme w, Symbol newValue);
    
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
    
}
