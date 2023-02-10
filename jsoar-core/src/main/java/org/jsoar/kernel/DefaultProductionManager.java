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
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.lhs.ConditionReorderer;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.parser.original.OriginalParser;
import org.jsoar.kernel.rete.ProductionAddResult;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rhs.ActionReorderer;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.smem.DefaultSemanticMemory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.Arguments;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class DefaultProductionManager implements ProductionManager
{
    private final Agent context;
    private SymbolFactoryImpl syms;
    private Rete rete;
    private ReinforcementLearning rl;
    private SourceLocation currentSourceLocation;
    
    private final ParserContext parserContext = new ParserContext()
    {
        @Override
        public Object getAdapter(Class<?> klass)
        {
            if(klass.equals(SourceLocation.class))
            {
                return currentSourceLocation;
            }
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
    private Map<String, Production> productionsByName = new HashMap<String, Production>();
    
    public DefaultProductionManager(Agent context)
    {
        this.context = context;
    }
    
    public void initialize()
    {
        this.syms = Adaptables.require(getClass(), context, SymbolFactoryImpl.class);
        this.rete = Adaptables.require(getClass(), context, Rete.class);
        this.rl = Adaptables.require(getClass(), context, ReinforcementLearning.class);
    }
    
    /**
     * <p>init_soar.cpp:297:reset_statistics
     */
    public void resetStatistics()
    {
        // reset_production_firing_counts(thisAgent);
        for(Production p : this.productionsByName.values())
        {
            p.resetFiringCount();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
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
        p.reorder(this.syms.getVariableGenerator(),
                new ConditionReorderer(this.syms.getVariableGenerator(), context.getTrace(), context.getMultiAttributes(), p.getName()),
                new ActionReorderer(context.getPrinter(), p.getName()),
                false);
        
        validateLongTermIdentifiersInProduction(p);
        
        // Tell RL about the new production
        rl.addProduction(p);
        
        // Production is added to the rete by the chunker
        
        productionsByType.get(p.getType()).add(p);
        productionsByName.put(p.getName(), p);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ProductionManager#exciseProduction(org.jsoar.kernel.Production, boolean)
     */
    @Override
    public void exciseProduction(Production prod, boolean print_sharp_sign)
    {
        context.getEvents().fireEvent(new ProductionExcisedEvent(context, prod));
        
        productionsByType.get(prod.getType()).remove(prod);
        productionsByName.remove(prod.getName());
        
        rl.exciseProduction(prod);
        
        if(print_sharp_sign)
        {
            context.getPrinter().print("#").flush();
        }
        if(prod.getReteNode() != null)
        {
            this.rete.excise_production_from_rete(prod);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ProductionManager#getProduction(java.lang.String)
     */
    @Override
    public Production getProduction(String name)
    {
        return productionsByName.get(name);
    }
    
    /*
     * (non-Javadoc)
     * 
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
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ProductionManager#getParser()
     */
    @Override
    public Parser getParser()
    {
        return parser;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ProductionManager#setParser(org.jsoar.kernel.parser.Parser)
     */
    @Override
    public void setParser(Parser parser)
    {
        Arguments.checkNotNull(parser, "parser");
        this.parser = parser;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ProductionManager#loadProduction(java.lang.String)
     */
    @Override
    public Production loadProduction(String productionBody) throws ReordererException, ParserException
    {
        return loadProduction(productionBody, DefaultSourceLocation.UNKNOWN);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ProductionManager#loadProduction(java.lang.String, org.jsoar.util.SourceLocation)
     */
    @Override
    public Production loadProduction(String productionBody,
            SourceLocation location) throws ReordererException, ParserException
    {
        Arguments.checkNotNull(location, "location");
        this.currentSourceLocation = location;
        final StringReader reader = new StringReader(productionBody);
        final Production p = parser.parseProduction(parserContext, reader);
        
        if(p.getType() == ProductionType.CHUNK || p.getType() == ProductionType.JUSTIFICATION)
        {
            addChunk(p);
        }
        else
        {
            addProduction(p, true);
        }
        return p;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ProductionManager#addProduction(org.jsoar.kernel.Production, boolean)
     */
    public ProductionAddResult addProduction(Production p, boolean reorder_nccs) throws ReordererException
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
        Production existing = getProduction(p.getName());
        if(existing != null)
        {
            exciseProduction(existing, context.getTrace().isEnabled(Category.LOADING));
        }
        
        // Reorder the production
        p.reorder(this.syms.getVariableGenerator(),
                new ConditionReorderer(this.syms.getVariableGenerator(), context.getTrace(), context.getMultiAttributes(), p.getName()),
                new ActionReorderer(context.getPrinter(), p.getName()),
                reorder_nccs);
        
        validateLongTermIdentifiersInProduction(p);
        
        // Tell RL about the new production
        rl.addProduction(p);
        
        // Add it to the rete.
        ProductionAddResult result = this.rete.add_production_to_rete(p);
        
        // from parser.cpp
        if(result == ProductionAddResult.DUPLICATE_PRODUCTION)
        {
            exciseProduction(p, false);
            return result;
        }
        
        productionsByType.get(p.getType()).add(p);
        productionsByName.put(p.getName(), p);
        
        context.getEvents().fireEvent(new ProductionAddedEvent(context, p));
        
        return result;
    }
    
    /**
     * Performs semantic memory validation of a production. Throws an exception
     * if the production is invalid.
     * 
     * <p>Extracted from production.cpp:make_production.
     * 
     * @param p the production to check
     * @throws IllegalArgumentException if the production is invalid
     */
    private void validateLongTermIdentifiersInProduction(Production p)
    {
        if(p.getType() != ProductionType.JUSTIFICATION &&
                !DefaultSemanticMemory.smem_valid_production(p.getFirstCondition(), p.getFirstAction()))
        {
            throw new IllegalArgumentException("Ungrounded LTI in production: " + p);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
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
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ProductionManager#getTotalProductions()
     */
    @Override
    public int getProductionCount()
    {
        return productionsByName.size();
    }
    
    /**
     * (Internal method for supporting ReteNetReader. Incorrect usage can result in the production manager
     * no longer being consistent. You probably don't want to call this.)
     * 
     * Adds a production into the "production by name" and "production by type" maps.
     * 
     * @param p the production to note.
     */
    public void addProductionToNameTypeMaps(Production p)
    {
        productionsByType.get(p.getType()).add(p);
        productionsByName.put(p.getName(), p);
    }
}
