/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.symbols;

import org.jsoar.util.Arguments;

/**
 * @author ray
 */
public class Symbols
{
    private Symbols() {}
    
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
            return factory.createIdentifier(((Character) value).charValue());
        }
        if(value instanceof Double)
        {
            return factory.createDouble(((Double) value).doubleValue());
        }
        if(value instanceof Float)
        {
            return factory.createDouble(((Float) value).doubleValue());
        }
        if(value instanceof Integer)
        {
            return factory.createInteger(((Integer) value).intValue());
        }
        return factory.createString(value.toString());
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
