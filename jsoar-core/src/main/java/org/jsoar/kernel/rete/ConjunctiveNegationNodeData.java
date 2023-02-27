/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

/**
 * Data for cn and cn_partner nodes only
 * 
 * <p>rete.cpp:378
 * 
 * @author ray
 */
class ConjunctiveNegationNodeData implements BReteNodeData
{
    /**
     * The partner, always of type {@link ReteNodeType#CN_PARTNER_BNODE}
     */
    ReteNode partner;
    
    /**
     * @return a copy of this object
     */
    public ConjunctiveNegationNodeData copy()
    {
        ConjunctiveNegationNodeData n = new ConjunctiveNegationNodeData();
        n.partner = this.partner;
        
        return n;
    }
}
