/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2009
 */
package org.jsoar.kernel.io.xml;

import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.Arguments;

/**
 * Implementation of {@link XmlWmeFactory} that creates WMEs in a RHS function.
 * 
 * @author ray
 */
public class RhsFunctionXmlWmeFactory implements XmlWmeFactory
{
    private final RhsFunctionContext context;
    
    /**
     * Constrcut a WME factory that uses the given {@link RhsFunctionContext} object
     * to construct WMEs.
     * 
     * @param context the RHS function context to use to create WMEs
     */
    public RhsFunctionXmlWmeFactory(RhsFunctionContext context)
    {
        Arguments.checkNotNull(context, "context");
        this.context = context;
    }

    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.xml.XmlWmeFactory#getSymbols()
     */
    @Override
    public SymbolFactory getSymbols()
    {
        return context.getSymbols();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.xml.XmlWmeFactory#addWme(org.jsoar.kernel.symbols.Identifier, org.jsoar.kernel.symbols.Symbol, org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public void addWme(Identifier id, Symbol attr, Symbol value)
    {
        context.addWme(id, attr, value);
    }

}
