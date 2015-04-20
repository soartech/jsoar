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

/**
 * SimpleMatcher creates a rete that you can put productions and wmes into and check which productions match
 * This class has its own rete and symbol factory, so it will not interfere with any agent's rete or symbol factory
 * It has no logic for what to do when a production matches, no decision cycle, etc. It just allows you to check for matches.
 * 
 * Possible enhancements
 * increase efficiency of wme removals
 *   approach 1: return timetag of new wme for use in efficient wme removal (requires user to create structure to store timetags)
 *   approach 2: keep map of wme -> wmeImpl, so can directly look up wme to remove (requires user keep reference to original wmes around)
 *   approach 3: return new wme copy for use in efficient wme removal (requires user to create structure to store new wmes)
 * allow user to register rete listener (so can be directly notified of production matches). 
 * change isMatching to numMatches. Requires adding code to listener to increment/decrement counter on match/unmatch. (Possibly more efficient than getting PartialMatches structure)
 * 
 *  
 */
public class SimpleMatcher
{
    private final SymbolFactoryImpl syms = new SymbolFactoryImpl();
    private final Listener listener = new Listener();
    private final Rete rete = new Rete(Trace.createStdOutTrace().enableAll(), syms);
    private final Map<String, Production> productions = new HashMap<String, Production>();
    
    /**
     * Creates an instance of SimpleMatcher.
     */
    public SimpleMatcher()
    {
        rete.setReteListener(listener);
    }
    
    /**
     * Adds a production to the rete.
     * @param s Production in standard Soar syntax without the "sp" or braces. Note that the RHS is currently ignored (this just does matches)
     * @return A Production instance
     * @throws ParserException If syntax error or duplicate production already exists in rete
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
        
        final Production p = parser.parseProduction(context, reader);
        final ProductionAddResult result = rete.add_production_to_rete(p);
        
        if(result == ProductionAddResult.DUPLICATE_PRODUCTION)
        {
            throw new IllegalArgumentException("duplicate production " + p.getName());
        }
        
        productions.put(p.getName(), p);
        return p;
    }
    
    /**
     * Removes the specified production from the rete
     * @param p A Production instance that is already in the rete
     */
    public void removeProduction(Production p)
    {
        rete.excise_production_from_rete(p);
        productions.remove(p.getName());
    }
    
    /**
     * Removes the production with the specified name from the rete 
     * @param productionName The name of the production to remove
     * @throws IllegalArgumentException If production with specified name is not in rete
     */
    public void removeProduction(String productionName) throws IllegalArgumentException
    {
        final Production p = productions.get(productionName);
        if(p == null)
        {
            throw new IllegalArgumentException("production " + productionName + " doesn't exist");
        }
        
        removeProduction(p);
    }
    
    /**
     * Removes all productions from the rete
     */
    public void removeAllProductions()
    {
        for(Production p : productions.values())
        {
            removeProduction(p);
        }
    }
    
    /**
     * Creates a copy of the wme and adds it to the SimpleMatcher's rete
     * Need to create a copy because a Wme can't be in multiple retes
     * @param w Wme to add to the rete
     */
    public void addWme(Wme w)
    {
        final IdentifierImpl id = copySymbol(syms, w.getIdentifier()).asIdentifier();
        final SymbolImpl attr = copySymbol(syms, w.getAttribute());
        final SymbolImpl value = copySymbol(syms, w.getValue());
        final WmeImpl wme = new WmeImpl(id, attr, value, false, 0); // TODO: if want to support timetags, autoincrement here
        rete.add_wme_to_rete(wme);
    }
    
    /**
     * Finds a matching Wme in the rete and removes it
     * @param w Wme instance
     * @throws IllegalArgumentException If Wme is not in rete
     */
    public void removeWme(Wme w) throws IllegalArgumentException
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
    
    /**
     * Removes all wmes from the rete
     */
    public void removeAllWmes()
    {
        // need to make a copy since remove_wme_from_rete will destructively modify the collection returned by rete.getAllWmes
        final Set<WmeImpl> wmes = new HashSet<WmeImpl>(rete.getAllWmes());

        for(WmeImpl w : wmes)
        {
            rete.remove_wme_from_rete(w);
        }
    }
    
    /**
     * @param p Production instance to check for matches
     * @return true if production argument current matches, otherwise false
     */
    public int getNumberMatches(Production p)
    {
        Integer numMatches = listener.matching.get(p);
        if(numMatches != null)
        {
            return numMatches;
        }
        else
        {
            return 0;
        }
    }
    
    /**
     * @param productionName Name of Production to check for matches
     * @return true if production argument current matches, otherwise false
     * @throws IllegalArgumentException if Production instance is not in rete
     */
    public int getNumberMatches(String productionName) throws IllegalArgumentException
    {
        final Production p = productions.get(productionName);
        if(p == null)
        {
            throw new IllegalArgumentException("production " + productionName + " doesn't exist");
        }
        
        return getNumberMatches(p);
    }
    
    /**
     * @param p Production instance to get PartialMatches for
     * @return A PartialMatches instance representing the partial matches for the specified Production
     */
    public PartialMatches getMatches(Production p)
    {
        return rete.getPartialMatches(p.getReteNode());
    }
    
    /**
     * @param productionName Name of Production to get PartialMatches for
     * @return A PartialMatches instance representing the partial matches for the specified Production
     * @throws IllegalArgumentException if the specified production doesn't exist
     */
    public PartialMatches getMatches(String productionName) throws IllegalArgumentException
    {
        final Production p = productions.get(productionName);
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
            copySym = syms.findOrCreateIdentifierExact(sId.getNameLetter(), sId.getNameNumber());
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
        Map<Production, Integer> matching = new HashMap<Production, Integer>();
        
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
            Integer i;
            final Production p = node.b_p().prod;
            if(matching.containsKey(p))
            {
                i = matching.get(p);
                ++i;
            }
            else
            {
                i = 1;
            }
            matching.put(p, i);
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_removal(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_removal(Rete rete, ReteNode node, Token tok, WmeImpl w)
        {
            final Production p = node.b_p().prod;
            Integer i = matching.get(p);
            --i;
            if(i>0)
            {
                matching.put(p, i);
            }
            else
            {
                matching.remove(p);
            }
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
