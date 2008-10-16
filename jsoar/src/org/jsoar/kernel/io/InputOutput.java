/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * @author ray
 */
public interface InputOutput
{
    SymbolFactory getSymbolFactory();
    
    Wme addInputWme(Identifier id, Symbol attr, Symbol value);
    
    void removeInputWme(Wme w);
    
    Identifier getInputLink();
    
    Identifier getOutputLink();
    
}
