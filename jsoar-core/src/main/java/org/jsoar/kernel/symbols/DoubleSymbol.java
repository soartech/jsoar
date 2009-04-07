/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.symbols;

/**
 * A symbol whose value is a double.
 * 
 * @author ray
 */
public interface DoubleSymbol extends Symbol
{
    /**
     * @return The value of the symbol
     * @see Symbols#valueOf(Symbol)
     */
    double getValue();
}
