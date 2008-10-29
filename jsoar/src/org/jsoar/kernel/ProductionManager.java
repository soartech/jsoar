/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rhs.ReordererException;

/**
 * Public interface for accessing and manipulating productions in a Soar agent
 *  
 * @author ray
 */
public interface ProductionManager
{
    public void loadProduction(String productionBody) throws IOException, ReordererException, ParserException;
    
    /**
     * Add the given chunk or justification production to the agent. The chunk 
     * is reordered and registered, but it is <b>not</b> added to the rete 
     * network.
     * 
     * <p>This is part of a refactoring of make_production().
     * 
     * @param p The chunk to add
     * @throws ReordererException if there is an error while reordering the chunk
     * @throws IllegalArgumentException if the production is not a chunk or justification
     */
    public void addChunk(Production p) throws ReordererException;
    
    /**
     * Look up a production by name
     * 
     * @param name The name of the production
     * @return The production or <code>null</code> if not found
     */
    public Production getProduction(String name);

    /**
     * Returns a list of productions of a particular type, or all productions
     * if type is <code>null</code>
     * 
     * @param type Type of production, or <code>null</code> for all productions.
     * @return List of productions, ordered by type and then by order of addition
     */
    public List<Production> getProductions(ProductionType type);

    /**
     * 
     * <p>production.cpp:1595:excise_production
     * 
     * @param prod
     * @param print_sharp_sign
     */
    public void exciseProduction(Production prod, boolean print_sharp_sign);
    
    /**
     * @return an immutable map from production type to count
     */
    public Map<ProductionType, Integer> getProductionCounts();
    
    /**
     * @return count of all productions currently loaded in the agent
     */
    public int getProductionCount();
}
