/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jsoar.util.Arguments;

/**
 * Utility methods for dealing with {@link Symbol} and sub-classes.
 * 
 * @author ray
 */
public class Symbols
{
    private Symbols() {}
    
    /**
     * Convert an arbitrary Java object into a Soar symbol. 
     * 
     * <p>The conversion is according to the following rules:
     * <ul>
     * <li><code>null</code> - new {@link Identifier} with letter 'Z'
     * <li>Symbol - returns the symbol
     * <li>Character - {@link StringSymbol}
     * <li>Double, Float - {@link DoubleSymbol}
     * <li>Integer, Log, Short, Byte, AtomicInteger, AtomicLong - IntegerSymbol (note possible loss of data)
     * <li>All others - {@link StringSymbol} using Object.toString()
     * </ul>
     * 
     * @param factory the symbol factory to use
     * @param value the object to convert
     * @return new symbol
     */
    public static Symbol create(SymbolFactory factory, Object value)
    {
        Arguments.checkNotNull(factory, "factory");
        
        if(value == null)
        {
            return factory.createIdentifier('Z');
        }
        if(value instanceof Symbol)
        {
            return (Symbol) value;
        }
        if(value instanceof Character)
        {
            return factory.createString(value.toString());
        }
        if(value instanceof Double || value instanceof Float)
        {
            return factory.createDouble(((Number) value).doubleValue());
        }
        if(value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte ||
           value instanceof AtomicInteger || value instanceof AtomicLong)
        {
            return factory.createInteger(((Number) value).intValue());
        }
        return factory.createString(value.toString());
    }
    
    /**
     * Converts a list of arguments into a list of symbols of the appropriate type.
     * Conversion is according to the rules of {@link #create(SymbolFactory, Object)}.
     * 
     * @param factory The symbol factory to use
     * @param objects List of objects
     * @return List of symbols
     */
    public static List<Symbol> asList(SymbolFactory factory, Object... objects)
    {
        List<Symbol> result = new ArrayList<Symbol>(objects.length);
        for(Object o : objects)
        {
            result.add(create(factory, o));
        }
        return result;
    }
    
    public static char getFirstLetter(Object attr)
    {
        if(attr == null)
        {
            return 'Z';
        }
        String s = attr.toString();
        if(s.length() == 0)
        {
            return 'Z';
        }
        char c = s.charAt(0);
        if(!Character.isLetter(c))
        {
            return 'Z';
        }
        return Character.toUpperCase(c);
    }
}
