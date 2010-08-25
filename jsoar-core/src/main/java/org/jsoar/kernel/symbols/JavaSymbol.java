/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.symbols;

import org.jsoar.script.ScriptCommand;

/**
 * A symbol whose value is an arbitrary Java object. 
 * 
 * <p>This symbol is meant to support JSoar scripting.
 *  
 * @author ray
 * @see ScriptCommand
 */
public interface JavaSymbol extends Symbol
{
    /**
     * @return The value of the symbol
     * @see Symbols#valueOf(Symbol)
     */
    Object getValue();
}
