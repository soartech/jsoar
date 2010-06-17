/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.kernel;

import java.util.List;
import java.util.Map;

import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rete.ProductionAddResult;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.util.SourceLocation;

/**
 * Public interface for accessing and manipulating productions in a Soar agent
 *  
 * @author ray
 */
public interface ProductionManager
{
    /**
     * @return the currently installed parser. Never <code>null</code>.
     */
    Parser getParser();
    
    /**
     * Set the default parser used by this production manager when 
     * {@link #loadProduction(String)} is called.
     * 
     * @param parser the new parser, never <code>null</code>
     * @throws IllegalArgumentException if parser is <code>null</code>
     */
    void setParser(Parser parser);
    
    /**
     * Parse a <b>production body</b> and add it to the agent's rete network.
     * Note that the production body does not include the {@code sp} or surrounding
     * braces!
     * 
     * @param productionBody body of production with no {@code sp} or braces
     * @param location the source location of the production 
     * @return the parsed production
     * @throws ReordererException
     * @throws ParserException
     */
    public Production loadProduction(String productionBody, SourceLocation location) throws ReordererException, ParserException;
    
    /**
     * Convenience version of {@link #loadProduction(String, SourceLocation)} equivalent to 
     * {@code loadProduction(body, DefaultSourceLocation.UNKNOWN); }
     * 
     * @param productionBody
     * @return the parsed production
     * @throws ReordererException
     * @throws ParserException
     */
    public Production loadProduction(String productionBody) throws ReordererException, ParserException;
    
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
     * Add the given production to the agent. If a production with the same name
     * is already loaded, it is excised and replaced.
     * 
     * <p>This is part of a refactoring of make_production().
     * 
     * @param p The production to add
     * @param reorder_nccs if true, NCC conditions on the LHS are reordered
     * @throws ReordererException if there is an error during reordering
     * @throws IllegalArgumentException if p is a chunk or justification or if
     *      p (actual instance, not name) has already been added
     */
    public ProductionAddResult addProduction(Production p, boolean reorder_nccs) throws ReordererException;
    
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
