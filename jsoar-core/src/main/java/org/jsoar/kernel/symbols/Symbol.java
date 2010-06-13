/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import org.jsoar.util.adaptables.Adaptable;


/**
 * Base interface for Soar symbols.
 * 
 * @author ray
 * @see Symbols
 */
public interface Symbol extends Adaptable
{
    /**
     * Convert this symbol to a {@link DoubleSymbol}
     * 
     * @return the symbol, or <code>null</code> if it is not a {@link DoubleSymbol}
     */
    public DoubleSymbol asDouble();
    
    /**
     * Convert this symbol to a {@link IntegerSymbol}
     * 
     * @return the symbol, or <code>null</code> if it is not a {@link IntegerSymbol}
     */
    public IntegerSymbol asInteger();
    
    /**
     * Convert this symbol to a {@link StringSymbol}
     * 
     * @return the symbol, or <code>null</code> if it is not a {@link StringSymbol}
     */
    public StringSymbol asString();
    
    /**
     * Convert this symbol to a {@link Identifier}
     * 
     * @return the identifier, or <code>null</code> if it is not a {@link Identifier}
     */
    public Identifier asIdentifier();
    
    /**
     * Convert this symbol to a {@link JavaSymbol}
     * 
     * @return the symbol, or <code>null</code> if it is not a {@link JavaSymbol}
     */
    public JavaSymbol asJava();
}
