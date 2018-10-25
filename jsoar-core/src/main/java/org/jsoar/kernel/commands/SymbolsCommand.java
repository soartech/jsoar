/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.JavaSymbol;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "symbols" command.
 * 
 * @author ray
 */
public class SymbolsCommand implements SoarCommand
{
    final SymbolFactoryImpl syms;
    
    public SymbolsCommand(Agent agent)
    {
        this.syms = Adaptables.adapt(agent, SymbolFactoryImpl.class);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        final List<Symbol> all = syms.getAllSymbols();
        final StringBuilder result = new StringBuilder();
        printSymbolsOfType(result, all, Identifier.class);
        printSymbolsOfType(result, all, StringSymbol.class);
        printSymbolsOfType(result, all, IntegerSymbol.class);
        printSymbolsOfType(result, all, DoubleSymbol.class);
        printSymbolsOfType(result, all, Variable.class);
        printSymbolsOfType(result, all, JavaSymbol.class);
        
        return result.toString();
    }
    @Override
    public Object getCommand() {
        //todo - when implementing picocli, return the runnable
        return null;
    }
    private <T extends Symbol> void printSymbolsOfType(StringBuilder result, List<Symbol> all, Class<T> klass)
    {
        final List<String> asStrings = collectSymbolsOfType(all, klass);
        result.append("--- " + klass + " (" + asStrings.size() + ") ---\n");
        Collections.sort(asStrings);
        for(String s : asStrings)
        {
            result.append(s);
            result.append('\n');
        }
    }
    
    private <T extends Symbol> List<String> collectSymbolsOfType(List<Symbol> in, Class<T> klass)
    {
        final List<String> result = new ArrayList<String>();
        for(Symbol s : in)
        {
            if(klass.isInstance(s))
            {
                result.add(s.toString());
            }
        }
        return result;
    }

}
