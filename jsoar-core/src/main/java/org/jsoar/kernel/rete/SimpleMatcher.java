/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 1, 2010
 */
package org.jsoar.kernel.rete;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.parser.original.OriginalParser;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;

/*
 * SimpleMatcher creates a rete that you can put productions and wmes into and check which productions match
 * This class has its own rete and symbol factory, so it will not interfere with any agent's rete or symbol factory
 * It has no logic for what to do when a production matches, no decision cycle, etc. It just allows you to check for matches.
 */
public class SimpleMatcher
{
    private final SymbolFactoryImpl syms = new SymbolFactoryImpl();;
    private final Listener listener = new Listener();
    private final Rete rete = new Rete(Trace.createStdOutTrace().enableAll(), syms);;
    private final Map<String, Production> productions = new HashMap<String, Production>();
    
    public SimpleMatcher()
    {
        rete.setReteListener(listener);
    }
    
    /*
     * Adds a production to the rete
     * Throws an exception if duplicate production (SimpleMatcher has no logic for dealing with that)
     * Could return null if syntax error in production string?
     */
    public Production addProduction(String s) throws ParserException
    {
        final OriginalParser parser = new OriginalParser();
        final StringReader reader = new StringReader(s);
        final ParserContext context = new ParserContext() {

            @Override
            public Object getAdapter(Class<?> klass)
            {
                if(klass.equals(SymbolFactoryImpl.class))
                {
                    return syms;
                }
                else if(klass.equals(RhsFunctionManager.class))
                {
                    return new RhsFunctionManager(rhsFuncContext);
                }
                else if(klass.equals(Printer.class))
                {
                    return Printer.createStdOutPrinter();
                }
                return null;
            }
            
        };
        
        Production p = parser.parseProduction(context, reader);
        ProductionAddResult result = rete.add_production_to_rete(p);
        
        if(result==ProductionAddResult.DUPLICATE_PRODUCTION)
        {
            throw new IllegalArgumentException("duplicate production " + p.getName());
        }
        
        productions.put(p.getName(), p);
        return p;
    }
    
    /*
     * Removes the specified production from the rete
     */
    public void removeProduction(Production p)
    {
        rete.excise_production_from_rete(p);
        productions.remove(p);
    }
    
    /*
     * Removes the production with the specified name from the rete 
     */
    public void removeProduction(String productionName)
    {
        Production p = productions.get(productionName);
        if(p == null)
        {
            throw new IllegalArgumentException("production " + productionName + " doesn't exist");
        }
        
        removeProduction(p);
    }
    
    /*
     * Removes all productions from the rete
     */
    public void removeAllProductions()
    {
        for(Production p : productions.values())
        {
            removeProduction(p);
        }
    }
    
    /*
     * Creates a copy of the wme and adds it to the SimpleMatcher's rete
     */
    public void addWme(Wme w)
    {
        final IdentifierImpl id = (IdentifierImpl) syms.findOrCreateIdentifier(w.getIdentifier().getNameLetter(), w.getIdentifier().getNameNumber());
        final SymbolImpl attr = copySymbol(syms, w.getAttribute());
        final SymbolImpl value = copySymbol(syms, w.getValue());;
        final WmeImpl wme = new WmeImpl(id, attr, value, false, 0);
        rete.add_wme_to_rete(wme);
    }
    
    /*
     * Removes the wme that matches the specified wme from the rete
     * Throws an exception if no match?
     */
    public void removeWme(Wme w)
    {
        for(WmeImpl matcherWme : rete.getAllWmes())
        {
            if(Wmes.equalByValue(w, matcherWme))
            {
                rete.remove_wme_from_rete(matcherWme);
                return;
            }
        }
        
        throw new IllegalArgumentException("wme " + w.toString() + " not in rete");
    }
    
    /*
     * Removes all wmes from the rete
     */
    public void removeAllWmes()
    {
        int numWmes = rete.getAllWmes().size();
        for(int i=0; i<numWmes; ++i)
        {
            rete.remove_wme_from_rete(rete.getAllWmes().iterator().next());
        }
    }
    
    /*
     * Returns true if production argument current matches, otherwise false
     */
    public boolean isMatching(Production p)
    {
        return listener.matching.contains(p);
    }
    
    /*
     * Returns true if production with the specified name matches, otherwise false
     * Throws an exception if the specified production doesn't exist in the rete
     */
    public boolean isMatching(String productionName)
    {
        Production p = productions.get(productionName);
        if(p == null)
        {
            throw new IllegalArgumentException("production " + productionName + " doesn't exist");
        }
        
        return isMatching(p);
    }
    
    /*
     * Returns PartialMatches structure for specified production
     */
    public PartialMatches getMatches(Production p)
    {
        return rete.getPartialMatches(p.getReteNode());
    }
    
    /*
     * Returns PartialMatches structure for production with specified name
     * Throws an exception if the specified production doesn't exist
     */
    public PartialMatches getMatches(String productionName)
    {
        Production p = productions.get(productionName);
        if(p == null)
        {
            throw new IllegalArgumentException("production " + productionName + " doesn't exist");
        }
        
        return getMatches(p);
    }
    
    private SymbolImpl copySymbol(final SymbolFactoryImpl syms, final Symbol origSym)
    {
        final SymbolImpl copySym;
        final Identifier sId = origSym.asIdentifier();

        if(sId != null)
        {
            copySym = syms.findOrCreateIdentifier(sId.getNameLetter(), sId.getNameNumber());
        }
        else
        {
            copySym = (SymbolImpl) syms.importSymbol(origSym);
        }
        
        return copySym;
    }
    
    private RhsFunctionContext rhsFuncContext = new RhsFunctionContext() {

        @Override
        public SymbolFactory getSymbols()
        {
            return syms;
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionContext#addWme(org.jsoar.kernel.symbols.Identifier, org.jsoar.kernel.symbols.Symbol, org.jsoar.kernel.symbols.Symbol)
         */
        @Override
        public Void addWme(Identifier id, Symbol attr, Symbol value)
        {
            throw new UnsupportedOperationException("This test implementation of RhsFunctionContext doesn't support addWme");
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionContext#getProductionBeingFired()
         */
        @Override
        public Production getProductionBeingFired()
        {
            return null;
        }
    };
    
    private class Listener implements ReteListener
    {
        Set<Production> matching = new HashSet<Production>();
        
        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#finishRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public boolean finishRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
        {
            return false;
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_addition(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_addition(Rete rete, ReteNode node, Token tok, WmeImpl w)
        {
            matching.add(node.b_p.prod);
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_removal(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_removal(Rete rete, ReteNode node, Token tok, WmeImpl w)
        {
            matching.remove(node.b_p.prod);
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#startRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public void startRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
        {
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#removingProductionNode(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public void removingProductionNode(Rete rete, ReteNode p_node)
        {
        }
    }
    
    
}
