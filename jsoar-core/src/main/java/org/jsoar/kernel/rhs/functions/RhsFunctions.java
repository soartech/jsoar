/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;

/**
 * Various utility methods for working with RHS functions
 * 
 * @author ray
 */
public final class RhsFunctions
{

    private RhsFunctions()
    {
    }
    
    /**
     * Check the argument count for a RHS function. Throws a {@link RhsFunctionException}
     * if the argument count isn't valid.
     * 
     * @param handler the handler
     * @param arguments the incoming argument list to check
     * @throws RhsFunctionException if the argument list doesn't comply to the
     *      min/max args of the handler
     */
    public static void checkArgumentCount(RhsFunctionHandler handler, 
                                          List<Symbol> arguments) throws RhsFunctionException
    {
        checkArgumentCount(handler.getName(), arguments, 
                           handler.getMinArguments(), handler.getMaxArguments());
    }
    
    /**
     * Check the argument count for an argument list. Throws a
     * {@link RhsFunctionException} if the count isn't valid.
     * 
     * @param name the name of the function, used to construct error messages
     * @param arguments the incomint argument list to check
     * @param min the min number of expected arguments, inclusive
     * @param max the max number of expected arguments, inclusive
     * @throws RhsFunctionException if the argument list doesn't comply to the
     *      min/max args
     */
    public static void checkArgumentCount(String name, List<Symbol> arguments, int min, int max) throws RhsFunctionException
    {
        int count = arguments.size();
        
        if(min == max && count != min)
        {
            throw new RhsFunctionException("'" + name + "' function called with " + count + " arguments. Expected " + min + ".");
        }
        if(count < min)
        {
            throw new RhsFunctionException("'" + name + "' function called with " + count + " arguments. Expected at least " + min + ".");
        }
        if(count > max)
        {
            throw new RhsFunctionException("'" + name + "' function called with " + count + " arguments. Expected at most " + max + ".");
        }
    }
    
    /**
     * Check that all arguments in an argument list are numeric, i.e. a {@link IntegerSymbol}
     * or {@link DoubleSymbol}
     * 
     * @param name the function name, used to construct error messages
     * @param arguments the incoming argument list to check
     * @throws RhsFunctionException if any arguments are non-numeric
     */
    public static void checkAllArgumentsAreNumeric(String name, List<Symbol> arguments) throws RhsFunctionException
    {
        for (Symbol arg : arguments) 
        {
            if(arg.asInteger() == null && arg.asDouble() == null)
            {
                throw new RhsFunctionException("non-number (" + arg + ") passed to '" + name + "' function");
            }
        }
    }
    
    /**
     * Given an iterator over a list of values, constructs a linked list of WMEs
     * using {@link Symbols#create(SymbolFactory, Object)} to convert the values. 
     * 
     * <p>For example, this code
     * <pre>{@code
     * RhsFunctions.createLinkedList(context, Arrays.asList("hi", "bye", 12345, 3.14).iterator());
     * }</pre>
     * 
     * <p>would yield this WM structure:
     * 
     * <pre>{@code
     * ^value |hi|
     * ^next
     *    ^value |bye|
     *    ^next
     *        ^value 12345
     *        ^next
     *            ^value 3.14
     * }</pre>
     * 
     * @param context the RHS function context
     * @param values iterator over the list of values 
     * @return
     */
    public static Identifier createLinkedList(RhsFunctionContext context, Iterator<?> values)
    {
        return createLinkedList(context, values, "next", "value");
    }
    
    /**
     * Same as {@link #createLinkedList(RhsFunctionContext, Iterator)}, but the name of 
     * the "value" and "next" attributes in the list can be specified
     *  
     * @param context the RHS function context
     * @param values iterator over the list of values 
     * @param nextName the name of the next attribute
     * @param valueName the name of the value attribute
     * @return
     */
    public static Identifier createLinkedList(RhsFunctionContext context, Iterator<?> values, String nextName, String valueName)
    {
        final SymbolFactory syms = context.getSymbols();
        final Symbol nextSym = syms.createString("next");
        final Symbol valueSym = syms.createString("value");
        
        Identifier head = null;
        Identifier last = null;
        while(values.hasNext())
        {
            final Object o = values.next();
            final Identifier current = syms.createIdentifier('N');
            context.addWme(current, valueSym, Symbols.create(syms, o));
            
            if(head == null)
            {
                head = current;
            }
            if(last != null)
            {
                context.addWme(last, nextSym, current);
            }
            last = current;
        }
        return head;
    }
    
    static double asDouble(Symbol sym)
    {
        IntegerSymbol ic = sym.asInteger();
        
        return ic != null ? ic.getValue() : sym.asDouble().getValue();
    }
    
}
