/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 24, 2009
 */
package org.jsoar.kernel.io.beans;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Symbols;

/**
 * Interface passed to {@link SoarBeanOutputHandler} with additinoal
 * context information.
 * 
 * @author ray
 */
public interface SoarBeanOutputContext
{
    /**
     * @return the input/output object
     */
    InputOutput getInputOutput();
    
    /**
     * @return the root WME of the output command. The id of the WME will
     *  always be the output-link.
     */
    Wme getCommand();
    
    /**
     * Set the {@code ^status} WME on the output command to the given value.
     * The value is interpreted according to the rules of {@link Symbols#create(org.jsoar.kernel.symbols.SymbolFactory, Object)}.
     * 
     * @param status the status value, interpreted according to the rules of 
     *   {@link Symbols#create(org.jsoar.kernel.symbols.SymbolFactory, Object)}.
     */
    void setStatus(Object status);
    
    /**
     * Convenience form of {@link #setStatus(Object)} which adds the WME
     * {@code ^status complete} to the output command
     */
    void setStatusComplete();
}
