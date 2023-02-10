/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 9, 2009
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;

/**
 * Helper methods associated with {@link InputWme}s.
 * 
 * @see InputWme
 * @see InputOutput
 * @author ray
 */
public class InputWmes
{
    /**
     * Create a new input Wme with the given id, attribute and value. The
     * attribute and value are created according to the rules of
     * {@link Symbols#create(SymbolFactory, Object)}. This method should
     * only be called from inside {@link InputEvent}.
     * 
     * @param io the input/output interface
     * @param id the identifier of the wme
     * @param attr the attribute
     * @param value the value
     * @return the new input wme.
     * @see Symbols#create(SymbolFactory, Object)
     */
    public static InputWme add(InputOutput io, Identifier id, Object attr, Object value)
    {
        final SymbolFactory syms = io.getSymbols();
        return io.addInputWme(id, Symbols.create(syms, attr), Symbols.create(syms, value));
    }
    
    /**
     * Convenience form of {@link #add(InputOutput, Identifier, Object, Object)}
     * which creates a new input wme directly on the input-link, i.e. <code>I2</code>.
     * 
     * @param io the input/output interface
     * @param attr the attribute
     * @param value the value
     * @return the new input wme
     * @see Symbols#create(SymbolFactory, Object)
     */
    public static InputWme add(InputOutput io, Object attr, Object value)
    {
        return add(io, io.getInputLink(), attr, value);
    }
    
    /**
     * Convenience form of {@link #add(InputOutput, Identifier, Object, Object)}
     * which creates a new input wme under the given input wme.
     * 
     * <p>For example, if {@code parent} is {@code (I2 ^contact C2)},
     * then the call {@code add(parent, "name", "catbird")} will create the wme
     * {@code (C2 ^name catbird)}
     * 
     * @param parent the parent WME. Its value <b>must</b> be an {@link Identifier}
     * @param attr the attribute
     * @param value the value
     * @return the new input wme
     * @throws IllegalArgumentException if {@code parent.getValue()} is not an
     *     {@link Identifier}
     * @see Symbols#create(SymbolFactory, Object)
     */
    public static InputWme add(InputWme parent, Object attr, Object value)
    {
        final Identifier id = parent.getValue().asIdentifier();
        if(id == null)
        {
            throw new IllegalArgumentException("value of parent must be an id, got: " + parent.getValue());
        }
        return add(parent.getInputOutput(), id, attr, value);
    }
    
    /**
     * Update the value of the given input wme with the given value according
     * to the rules of {@link Symbols#create(SymbolFactory, Object)}. This is
     * a convenience form of {@link InputWme#update(org.jsoar.kernel.symbols.Symbol)}.
     * 
     * @param wme the input wme to update
     * @param newValue the new value
     * @return the wme
     * @see InputWme#update(org.jsoar.kernel.symbols.Symbol)
     * @see Symbols#create(SymbolFactory, Object)
     */
    public static InputWme update(InputWme wme, Object newValue)
    {
        wme.update(Symbols.create(wme.getInputOutput().getSymbols(), newValue));
        return wme;
    }
    
}
