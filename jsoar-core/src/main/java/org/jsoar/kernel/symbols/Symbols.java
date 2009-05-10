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
    
    public static final Object NEW_ID = new String("*create a new identifier*");
    
    /**
     * Convert an arbitrary Java object into a Soar symbol. 
     * 
     * <p>The conversion is according to the following rules:
     * <ul>
     * <li>{@link #NEW_ID} - returns a new identifier symbol
     * <li>Symbol - returns the symbol
     * <li>Character - {@link StringSymbol}
     * <li>Double, Float - {@link DoubleSymbol}
     * <li>Integer, Long, Short, Byte, AtomicInteger, AtomicLong - IntegerSymbol (note possible loss of data)
     * <li>All others, including <code>null</code> - {@link JavaSymbol}
     * </ul>
     * 
     * @param factory the symbol factory to use
     * @param value the object to convert
     * @return new symbol
     */
    public static Symbol create(SymbolFactory factory, Object value)
    {
        Arguments.checkNotNull(factory, "factory");
        
        if(value == NEW_ID)
        {
            return factory.createIdentifier('Z');
        }
        if(value instanceof Symbol)
        {
            return (Symbol) value;
        }
        if(value instanceof Character || value instanceof String)
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
        return factory.createJavaSymbol(value);
    }
    
    /**
     * Convert a symbol's value to a Java object type. This is the reverse of
     * the procedure in {@link #create(SymbolFactory, Object)}.
     * 
     * <p>The following conversion rules are applied:
     * <ul>
     * <li>{@link Identifier} - returns the identifier unchanged
     * <li>{@link DoubleSymbol} - returns java.lang.Double
     * <li>{@link IntegerSymbol} - returns java.lang.Integer
     * <li>{@link StringSymbol} - returns java.lang.String
     * <li>{@link JavaSymbol} - returns {@link JavaSymbol#getValue()}, possibly <code>null</code>.
     * </ul>
     * 
     * @param sym The symbol to convert, not <code>null</code>
     * @return Symbol value as a Java object
     * @throws IllegalArgumentException if sym is <code>null</code>
     * @throws IllegalStateException if sym type is unknown 
     */
    public static Object valueOf(Symbol sym)
    {
        Arguments.checkNotNull(sym, "sym");
        
        Identifier id = sym.asIdentifier();
        if(id != null)
        {
            return id;
        }
        DoubleSymbol d = sym.asDouble();
        if(d != null)
        {
            return d.getValue();
        }
        IntegerSymbol i = sym.asInteger();
        if(i != null)
        {
            return i.getValue();
        }
        StringSymbol s = sym.asString();
        if(s != null)
        {
            return s.toString();
        }
        JavaSymbol js = sym.asJava();
        if(js != null)
        {
            return js.getValue();
        }
        throw new IllegalStateException("Unknown symbol type: " + sym.getClass());
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
    
    /**
     * Get the first letter of an attribute. Uses {@link Object#toString()}
     * and retrieves the first character. If {@code attr} is {@code null}, is
     * an empty string, or begins with a non-alphabetic character, {@code 'Z'} 
     * is returned.
     * 
     * @param attr an attribute name
     * @return first letter of attribute name, or {@code 'Z'}
     */
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
