/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.Arrays;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
public class StandardRhsFunctions
{
    private final Agent context;
    
    /**
     * Takes any number of arguments, and prints each one.
     *  
     * rhsfun.cpp:162:write_rhs_function_code
     */
    public final RhsFunctionHandler write = new AbstractRhsFunctionHandler("write") {

        @Override
        public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
        {
            for(Symbol arg : arguments)
            {
                context.getPrinter().print(arg.toString());
            }
            return null;
        }
    };
    
    /**
     * Just returns a sym_constant whose print name is a line feed.
     *  
     * rhsfun.cpp:189:crlf_rhs_function_code
     */
    public final RhsFunctionHandler crlf = new AbstractRhsFunctionHandler("crlf") {

        @Override
        public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
        {
            return syms.createString("\n");
        }
    };
    
    /**
     * List of all standard RHS function handlers
     */
    public final List<RhsFunctionHandler> all = Arrays.asList(write, crlf, new Plus(), new Multiply(), new Minus(), new FloatingPointDivide());

    /**
     * @param context
     */
    public StandardRhsFunctions(Agent context)
    {
        this.context = context;
        
        for(RhsFunctionHandler handler : all)
        {
            context.getRhsFunctions().registerHandler(handler);
        }
    }
    
    
}
