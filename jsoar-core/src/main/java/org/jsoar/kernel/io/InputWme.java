/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 8, 2009
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.events.InputCycleEvent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Interface for an input working memory element. 
 * 
 * <p>InputWmes are created with the {@link InputOutput#addInputWme(org.jsoar.kernel.symbols.Identifier, Symbol, Symbol)}
 * method. Unlike a normal {@link Wme}, an InputWme's value may change through 
 * the {@link #update(Symbol)} method. 
 * 
 * <p>Like a normal Wme, the symbols returned by {@link Wme#getIdentifier()} and
 * {@link Wme#getAttribute()} will never change. However, if {@link #update(Symbol)}
 * is called the values returned by {@link Wme#getValue()} and {@link Wme#getTimetag()}
 * may change.
 * 
 * <p>Note that an InputWme is not a normal Wme and you shouldn't expect casts 
 * from one to the other to work. Use adapters to correctly convert from a raw
 * Wme to an InputWme:
 * 
 * <pre>{@code
 *    InputWme iw = Adaptables.adapt(wme, InputWme.class);
 *    if(iw != null)
 *    {
 *       // It's an input Wme!
 *    }
 * }</pre>
 * 
 * @see InputWmes
 * @see InputOutput
 * @author ray
 */
public interface InputWme extends Wme
{
    /**
     * @return the InputOutput object that created this Wme.
     */
    InputOutput getInputOutput();
    
    /**
     * Update the value of this input WME. This should only be called during the
     * input phase, i.e. from within an {@link InputCycleEvent} callback.
     * 
     * @param newValue New value for the WME
     */
    void update(Symbol newValue);
    
    /**
     * Remove this input WME. This should only be called during the input
     * phase, i.e. from within an {@link InputCycleEvent} callback.
     * 
     * <p>io.cpp:243:remove_input_wme
     */
    void remove();
}
