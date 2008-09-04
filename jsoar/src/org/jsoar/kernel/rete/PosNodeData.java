/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

/**
 * 
 * Data for positive nodes only
 * 
 * rete.cpp:358
 * 
 * @author ray
 */
public class PosNodeData extends ReteNodeData
{
    /* --- dll of left-linked pos nodes from the parent beta memory --- */
    ReteNode next_from_beta_mem;
    boolean node_is_left_unlinked;
    ReteNode prev_from_beta_mem;
    
    /**
     * @return A shallow copy of this object
     */
    public PosNodeData copy()
    {
        PosNodeData n = new PosNodeData();
        n.next_from_beta_mem = this.next_from_beta_mem;
        n.node_is_left_unlinked = this.node_is_left_unlinked;
        n.prev_from_beta_mem = this.prev_from_beta_mem;
        return n;
    }

}
