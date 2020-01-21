/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * rhsfun_math.cpp:474:int_rhs_function_code
 * 
 * @author ray
 */
public class IntRhsFunction extends AbstractRhsFunctionHandler
{
    private static final Pattern number = Pattern.compile("^(-?\\d+).*?");
    
    public IntRhsFunction()
    {
        super("int", 1, 1);
    }

    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        final Symbol arg = arguments.get(0);
        final IntegerSymbol intSym = arg.asInteger();
        if(intSym != null)
        {
            return intSym;
        }
        final DoubleSymbol doubleSym = arg.asDouble();
        if(doubleSym != null)
        {
            return context.getSymbols().createInteger((long) doubleSym.getValue());
        }
        if(arg.asIdentifier() != null)
        {
            throw new RhsFunctionException("Identifier passed to int RHS function: " + arg);
        }
        final String string = arg.toString();
        try
        {
            return context.getSymbols().createInteger(parseString(string));
        }
        catch(NumberFormatException e)
        {
            throw new RhsFunctionException(arg + " is not a valid number.");
        }
    }
    
    /**
     * Cast strings like "3", "3.0", and "3.44" to
     * the appropriate (truncated) long value (3). 
     * 
     * Emulates the behavior of strtol() for non-digit characters by
     * truncating at the first non-digit character after an optional
     * leading minus (e.g., "-3.5e10" maps to -3).
     */
    private long parseString(String s)
    {
        Long lng;
        try
        {
            lng = Long.parseLong(s);
            return lng;
        }
        catch (NumberFormatException e)
        {
            // Truncate after the first non-digit character.
            // Handles mapping exponential notation (e.g., "45e-10" -> "45")
            // and also strtol()-style string handling ("123abc" -> "123").
            Matcher matcher = number.matcher(s);
            if (matcher.find() && matcher.groupCount() > 0)
            {
                lng = Long.parseLong(matcher.group(1));
            }
            else
            {
                lng = Long.parseLong(s);
            }
            return lng;
        }
    }
}
