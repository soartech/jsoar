/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 27, 2009
 */
package org.jsoar.kernel.memory;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * Interface use by objects that need to construct WMEs such as a 
 * RHS function or input generator
 * 
 * @param <T> the return type of {@link #addWme(Identifier, Symbol, Symbol)}.
 *      Typically {@code Void}, {@code Wme}, or {@code InputWme}
 * @author ray
 */
public interface WmeFactory<T>
{
    /**
     * @return a symbol factory to use to construct new symbols
     */
    SymbolFactory getSymbols();
    
    /**
     * Add a new WME. Note that this method does not return a new Wme. This is
     * because Wmes created in a RHS function are not actually "created" until
     * later so there is no WME to return.
     * 
     * @param id the id of the new WME
     * @param attr the attribute of the new WME
     * @param value the value of the new WME
     * @return the new WME, or possibly {@code Void} if the WME is not created
     *  immediately.
     */
    T addWme(Identifier id, Symbol attr, Symbol value);
}
