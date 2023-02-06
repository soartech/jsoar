/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.parser.original.Lexeme;
import org.jsoar.kernel.parser.original.Lexer;
import org.jsoar.util.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for dealing with {@link Symbol} and sub-classes.
 * 
 * @author ray
 */
public class Symbols
{
    private static final Logger logger = LoggerFactory.getLogger(Agent.class);
    private static final boolean WARN_ON_JAVA_SYMBOLS = Boolean.valueOf(System.getProperty("jsoar.warnOnJavaSymbols", "true"));

    public static final int IDENTIFIER_SYMBOL_TYPE = 1;
    public static final int SYM_CONSTANT_SYMBOL_TYPE = 2;
    public static final int INT_CONSTANT_SYMBOL_TYPE = 3;
    public static final int FLOAT_CONSTANT_SYMBOL_TYPE = 4;

    private static Pattern ID_PATTERN = Pattern.compile("^\\s*([a-zA-Z])(\\d+)\\s*$");
    
    public static int getSymbolType(Symbol sym)
    {
        if(sym.asIdentifier() != null)
        {
            return IDENTIFIER_SYMBOL_TYPE;
        }
        else if(sym.asString() != null)
        {
            return SYM_CONSTANT_SYMBOL_TYPE;
        }
        else if(sym.asInteger() != null)
        {
            return INT_CONSTANT_SYMBOL_TYPE;
        }
        else if(sym.asDouble() != null)
        {
            return FLOAT_CONSTANT_SYMBOL_TYPE;
        }
        throw new IllegalArgumentException("Don't know integer type of symbol " + sym);
    }

    private Symbols() {}
    
    /**
     * Sentinel value passed to {@link #create(SymbolFactory, Object)} to indicate
     * that a new identifier should be created.
     */
    public static final Object NEW_ID = new Object() {
        @Override
        public String toString() { return "*create a new identifier*"; }
    };
    
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
            return factory.createInteger(((Number) value).longValue());
        }
        
        // Landing here could very well be a bug (passing null, or something 
        // else by accident. So we print a warning just in case.
        if(WARN_ON_JAVA_SYMBOLS)
        {
            logger.warn("A Java symbol with value '" + value + "' is being created. " + 
                    "Are you sure this is what you want to do? " + 
                    "Disable this message with -Djsoar.warnOnJavaSymbols=false.");
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
    
    /**
     * Parse and find an identifier from a string, for example <code>S123</code>.
     * 
     * @param symbols the symbol factory
     * @param idString the id string
     * @return the identifier, or {@code null} if parse failed, or the id doesn't
     *      exist.
     */
    public static Identifier parseIdentifier(SymbolFactory symbols, String idString)
    {
        final Matcher matcher = ID_PATTERN.matcher(idString);
        if(matcher.matches())
        {
            final char letter = matcher.group(1).toUpperCase().charAt(0);
            try
            {
                final long number = Long.parseLong(matcher.group(2));
                return symbols.findIdentifier(letter, number);
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }
    /**
     * 
     * <p>sml_KernelHelpers.cpp:731:read_attribute_from_string
     * 
     * <p>TODO: This probably shouldn't be here because of the Agent dependency
     * 
     * @param agent the agent 
     * @param s the attribute as a string
     * @return the associated symbol, or {@code null} if not found.
     */
    public static Symbol readAttributeFromString(Agent agent, String s)
    {
        if(s == null)
        {
            return null;
        }
        
        if(s.length() > 0 && s.charAt(0) == '^')
        {
            s = s.substring(1);
        }
        
        try
        {
            final Lexer lexer = new Lexer(agent.getPrinter(), new StringReader(s));
            lexer.getNextLexeme();
            final Lexeme lexeme = lexer.getCurrentLexeme();
            if(lexeme == null)
            {
                return null;
            }
            final SymbolFactory syms = agent.getSymbols();
            final Symbol attr;
            switch(lexeme.type)
            {
            case SYM_CONSTANT: attr = syms.findString(lexeme.string); break;
            case INTEGER: attr = syms.findInteger(lexeme.int_val); break;
            case FLOAT:  attr = syms.findDouble(lexeme.float_val); break;
            case IDENTIFIER: attr = syms.findIdentifier(lexeme.id_letter, lexeme.id_number); break; 
            case VARIABLE: attr = agent.readIdentifierOrContextVariable(lexeme.string); break;
            default:
                return null;
            }
            return attr;
        }
        catch (IOException e)
        {
            return null;
        } 
    }
    
    /**
     * utilities.cpp:361:get_number_from_symbol
     * 
     * @param sym the symbol to convert
     * @return the value of the symbol as a double
     */
    public static double asDouble(Symbol sym)
    {
        final IntegerSymbol ic = sym.asInteger();
        if(ic != null)
        {
            return ic.getValue();
        }
        final DoubleSymbol d = sym.asDouble();
        if(d != null)
        {
            return d.getValue();
        }
        return 0.0;
    }

    public static boolean equalByValue(Symbol s1, Symbol s2)
    {
        Identifier i1 = s1.asIdentifier();
        Identifier i2 = s2.asIdentifier();

        // both are ids, so compare them
        if (i1 != null && i2 != null)
        {
            if (i1.getNameLetter() == i2.getNameLetter()
                    && i1.getNameNumber() == i2.getNameNumber())
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        // one is an id and the other is not, so they are not equal
        else if (i1 != null || i2 != null)
        {
            return false;
        }
        else
        {
            return Symbols.valueOf(s1).equals(Symbols.valueOf(s2));
        }
        
    }
}
