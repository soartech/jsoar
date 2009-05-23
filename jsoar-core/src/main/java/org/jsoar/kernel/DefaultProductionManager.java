/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.kernel;

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
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.parser.original.OriginalParser;
import org.jsoar.kernel.rete.ProductionAddResult;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rhs.ActionReorderer;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.Arguments;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class DefaultProductionManager implements ProductionManager
{
    private final Agent context;
    private final VariableGenerator variableGenerator;
    private Rete rete;
    
    private final ParserContext parserContext = new ParserContext() 
    {
        @Override
        public Object getAdapter(Class<?> klass)
        {
            return Adaptables.adapt(context, klass);
        }
    };
    
    private Parser parser = new OriginalParser();
    
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
        this.variableGenerator = new VariableGenerator(this.context.syms);
    }
    
    void initialize()
    {
        this.rete = Adaptables.adapt(context, Rete.class);
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
             p.reorder(this.variableGenerator, 
                       new ConditionReorderer(this.variableGenerator, context.getTrace(), context.getMultiAttributes(), p.getName().getValue()), 
                       new ActionReorderer(context.getPrinter(), p.getName().getValue()), 
                       false);

             // Tell RL about the new production
             context.rl.addProduction(p);
             
             // Production is added to the rete by the chunker

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
        
        context.getEventManager().fireEvent(new ProductionExcisedEvent(context, prod));
        
        productionsByType.get(prod.getType()).remove(prod);
        productionsByName.remove(prod.getName());

        context.rl.exciseProduction(prod);

        if (print_sharp_sign)
        {
            context.getPrinter().print("#").flush();
        }
        if (prod.getReteNode() != null)
        {
            this.rete.excise_production_from_rete(prod);
        }
        prod.production_remove_ref();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#getProduction(java.lang.String)
     */
    @Override
    public Production getProduction(String name)
    {
        StringSymbol sc = context.getSymbols().findString(name);
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
            result = new ArrayList<Production>(getProductionCount());
            for(Set<Production> ofType : productionsByType.values())
            {
                result.addAll(ofType);
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#getParser()
     */
    @Override
    public Parser getParser()
    {
        return parser;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#setParser(org.jsoar.kernel.parser.Parser)
     */
    @Override
    public void setParser(Parser parser)
    {
        Arguments.checkNotNull(parser, "parser");
        this.parser = parser;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#loadProduction(java.lang.String)
     */
    @Override
    public Production loadProduction(String productionBody) throws ReordererException, ParserException
    {
        final StringReader reader = new StringReader(productionBody);
        final Production p = parser.parseProduction(parserContext, reader);
        addProduction(p, true);
        return p;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.ProductionManager#addProduction(org.jsoar.kernel.Production, boolean)
     */
    public void addProduction(Production p, boolean reorder_nccs) throws ReordererException
    {
        if(productionsByName.values().contains(p))
        {
            throw new IllegalArgumentException("Production instance '" + p + " already added.");
        }
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
            exciseProduction(existing, context.getTrace().isEnabled(Category.LOADING));
        }

        // Reorder the production
        p.reorder(this.variableGenerator, 
                  new ConditionReorderer(this.variableGenerator, context.getTrace(), context.getMultiAttributes(), p.getName().getValue()), 
                  new ActionReorderer(context.getPrinter(), p.getName().getValue()), 
                  reorder_nccs);

        // Tell RL about the new production
        context.rl.addProduction(p);
        
        // Add it to the rete.
        ProductionAddResult result = this.rete.add_production_to_rete(p);
        
        // from parser.cpp
        if (result==ProductionAddResult.DUPLICATE_PRODUCTION) 
        {
            exciseProduction (p, false);
            return;
        }
        
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
        return productionsByName.size();
    }

}
