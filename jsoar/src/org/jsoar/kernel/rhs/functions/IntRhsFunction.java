/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

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
            return context.getSymbols().createInteger((int) doubleSym.getValue());
        }
        if(arg.asIdentifier() != null)
        {
            throw new RhsFunctionException("Identifier passed to int RHS function: " + arg);
        }
        final String string = arg.toString();
        try
        {
            return context.getSymbols().createInteger(Integer.parseInt(string));
        }
        catch(NumberFormatException e)
        {
            throw new RhsFunctionException(arg + " is not a valid integer.");
        }
    }
}
