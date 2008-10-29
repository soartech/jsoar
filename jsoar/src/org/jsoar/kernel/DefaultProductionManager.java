/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoar.kernel.events.ProductionAddedEvent;
import org.jsoar.kernel.events.ProductionExcisedEvent;
import org.jsoar.kernel.lhs.ConditionReorderer;
import org.jsoar.kernel.parser.Lexer;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rete.ProductionAddResult;
import org.jsoar.kernel.rhs.ActionReorderer;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.tracing.Trace.Category;

/**
 * @author ray
 */
public class DefaultProductionManager implements ProductionManager
{
    private final Agent context;
    private int totalProductions = 0;
    private EnumMap<ProductionType, Set<Production>> productionsByType = new EnumMap<ProductionType, Set<Production>>(ProductionType.class);
    {
        for(ProductionType type : ProductionType.values())
        {
            productionsByType.put(type, new LinkedHashSet<Production>());
        }
    }
    private Map<StringSymbol, Production> productionsByName = new HashMap<StringSymbol, Production>();

    DefaultProductionManager(Agent context)
    {
        this.context = context;
    }
    
    /**
     * <p>init_soar.cpp:297:reset_statistics
     */
    void resetStatistics()
    {
        // reset_production_firing_counts(thisAgent);
        for (Production p : this.productionsByName.values())
        {
            p.firing_count = 0;
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#addChunk(org.jsoar.kernel.Production)
     */
    @Override
    public void addChunk(Production p) throws ReordererException
    {
        if(p.getType() != ProductionType.CHUNK &&
                p.getType() != ProductionType.JUSTIFICATION)
             {
                 throw new IllegalArgumentException("Production '" + p + "' is not a chunk or justification");
             }
             
             // Reorder the production
             p.reorder(context.variableGenerator, 
                       new ConditionReorderer(context.variableGenerator, context.trace, context.multiAttrs, p.getName().getValue()), 
                       new ActionReorderer(context.getPrinter(), p.getName().getValue()), 
                       false);

             // Tell RL about the new production
             context.rl.addProduction(p);
             
             // Production is added to the rete by the chunker

             totalProductions++;
             productionsByType.get(p.getType()).add(p);
             productionsByName.put(p.getName(), p);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#exciseProduction(org.jsoar.kernel.Production, boolean)
     */
    @Override
    public void exciseProduction(Production prod, boolean print_sharp_sign)
    {
        // TODO if (prod->trace_firings) remove_pwatch (thisAgent, prod);
        
        if(print_sharp_sign)
        {
            context.getEventManager().fireEvent(new ProductionExcisedEvent(context, prod));
        }
        
        totalProductions--;
        productionsByType.get(prod.getType()).remove(prod);
        productionsByName.remove(prod.getName());

        context.rl.exciseProduction(prod);

        if (print_sharp_sign)
        {
            context.getPrinter().print("#");
        }
        if (prod.p_node != null)
        {
            context.rete.excise_production_from_rete(prod);
        }
        prod.production_remove_ref();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#getProduction(java.lang.String)
     */
    @Override
    public Production getProduction(String name)
    {
        StringSymbolImpl sc = context.syms.findString(name);
        return productionsByName.get(sc);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#getProductions(org.jsoar.kernel.ProductionType)
     */
    @Override
    public List<Production> getProductions(ProductionType type)
    {
        List<Production> result;
        if(type != null)
        {
            Set<Production> ofType = productionsByType.get(type);
            result = new ArrayList<Production>(ofType);
        }
        else
        {
            result = new ArrayList<Production>(totalProductions);
            for(Set<Production> ofType : productionsByType.values())
            {
                result.addAll(ofType);
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#loadProduction(java.lang.String)
     */
    @Override
    public void loadProduction(String productionBody) throws IOException, ReordererException, ParserException
    {
        StringReader reader = new StringReader(productionBody);
        Lexer lexer = new Lexer(context.getPrinter(), reader);
        Parser parser = new Parser(context.variableGenerator, lexer, context.operand2_mode);
        lexer.getNextLexeme();
        addProduction(parser.parse_production(), true);
    }
    
    /**
     * Add the given production to the agent. If a production with the same name
     * is already loaded, it is excised and replaced.
     * 
     * <p>This is part of a refactoring of make_production().
     * 
     * @param p The production to add
     * @param reorder_nccs if true, NCC conditions on the LHS are reordered
     * @throws ReordererException if there is an error during reordering
     * @throws IllegalArgumentException if p is a chunk or justification
     */
    private void addProduction(Production p, boolean reorder_nccs) throws ReordererException
    {
        if(p.getType() == ProductionType.CHUNK || p.getType() == ProductionType.JUSTIFICATION)
        {
            throw new IllegalArgumentException("Chunk or justification passed to addProduction: " + p);
        }
        
        // If there's already a prod with this name, excise it
        // Note, in csoar, this test was done in parse_production as soon as the name
        // of the production was known. We do this here so we can eliminate the
        // production field of StringSymbolImpl.
        Production existing = getProduction(p.getName().getValue());
        if (existing != null) 
        {
            exciseProduction(existing, context.trace.isEnabled(Category.LOADING));
        }

        // Reorder the production
        p.reorder(context.variableGenerator, 
                  new ConditionReorderer(context.variableGenerator, context.trace, context.multiAttrs, p.getName().getValue()), 
                  new ActionReorderer(context.getPrinter(), p.getName().getValue()), 
                  reorder_nccs);

        // Tell RL about the new production
        context.rl.addProduction(p);
        
        // Add it to the rete.
        ProductionAddResult result = context.rete.add_production_to_rete(p);
        
        // from parser.cpp
        if (result==ProductionAddResult.DUPLICATE_PRODUCTION) 
        {
            exciseProduction (p, false);
            return;
        }
        
        totalProductions++;
        productionsByType.get(p.getType()).add(p);
        productionsByName.put(p.getName(), p);
        
        context.getEventManager().fireEvent(new ProductionAddedEvent(context, p));
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#getProductionCounts()
     */
    @Override
    public Map<ProductionType, Integer> getProductionCounts()
    {
        Map<ProductionType, Integer> counts = new EnumMap<ProductionType, Integer>(ProductionType.class);
        for(ProductionType type : ProductionType.values())
        {
            counts.put(type, 0);
        }
        for(Map.Entry<ProductionType, Set<Production>> e : productionsByType.entrySet())
        {
            counts.put(e.getKey(), e.getValue().size());
        }
        return Collections.unmodifiableMap(counts);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#getTotalProductions()
     */
    @Override
    public int getProductionCount()
    {
        return totalProductions;
    }

}
