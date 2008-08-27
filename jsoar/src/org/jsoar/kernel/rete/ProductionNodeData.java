/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.MatchSetChange;

/**
 * data for production nodes only
 * 
 * rete.cpp:383
 * 
 * @author ray
 */
public class ProductionNodeData
{
    Production prod;                  /* the production */
    // TODO struct node_varnames_struct *parents_nvn;        /* records variable names */
    MatchSetChange tentative_assertions;   /* pending MS changes */
    MatchSetChange tentative_retractions;
    
    /**
     * @return Shallow copy of this object
     */
    public ProductionNodeData copy()
    {
        ProductionNodeData n = new ProductionNodeData();
        n.prod = this.prod;
        // TODO n.parents_nvn = this.parents_nvn (.copy())
        n.tentative_assertions = this.tentative_assertions;
        n.tentative_retractions = this.tentative_retractions;
        return n;
    }
}
