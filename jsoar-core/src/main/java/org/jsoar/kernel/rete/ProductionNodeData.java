/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;

/**
 * data for production nodes only
 * 
 * rete.cpp:383
 * 
 * @author ray
 */
public class ProductionNodeData implements BReteNodeData
{
    public Production prod;                  /* the production */
    NodeVarNames parents_nvn;         /* records variable names */
    
    // TODO: I think both of these fields belong in a Soar-specific sub-class
    // or something to decouple generic rete from Soar.
    MatchSetChange tentative_assertions;   // pending MS changes
    MatchSetChange tentative_retractions;
    
    public ProductionNodeData()
    {
    }
    
    public ProductionNodeData(ProductionNodeData other)
    {
        this.prod = other.prod;
        // TODO this.parents_nvn = other.parents_nvn (.copy())
        this.tentative_assertions = other.tentative_assertions;
        this.tentative_retractions = other.tentative_retractions;
    }
    /**
     * @return Shallow copy of this object
     */
    public ProductionNodeData copy()
    {
        return new ProductionNodeData(this);
    }
}
