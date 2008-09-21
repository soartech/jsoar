/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;
import org.jsoar.util.ListHead;

/**
 * data for production nodes only
 * 
 * rete.cpp:383
 * 
 * @author ray
 */
public class ProductionNodeData extends ReteNodeData
{
    public Production prod;                  /* the production */
    NodeVarNames parents_nvn;         /* records variable names */
    
    // TODO: I think both of these fields belong in a Soar-specific sub-class
    // or something to decouple generic rete from Soar.
    final ListHead<MatchSetChange> tentative_assertions;   // pending MS changes
    final ListHead<MatchSetChange> tentative_retractions;
    
    public ProductionNodeData()
    {
        this.tentative_assertions = new ListHead<MatchSetChange>();
        this.tentative_retractions = new ListHead<MatchSetChange>();
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
