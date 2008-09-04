/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

/**
 *  data for cn and cn_partner nodes only
 *  
 *  rete.cpp:378
 *  
 * @author ray
 */
public class ConjunctiveNegationNodeData extends ReteNodeData
{
    ReteNode partner;    /* cn, cn_partner point to each other */

    /**
     * @return
     */
    public ConjunctiveNegationNodeData copy()
    {
        ConjunctiveNegationNodeData n = new ConjunctiveNegationNodeData();
        n.partner = this.partner;
        
        return n;
    }

}
