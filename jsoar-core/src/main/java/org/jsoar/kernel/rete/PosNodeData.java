/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.util.ListItem;

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
    final ListItem<ReteNode> from_beta_mem;
    boolean node_is_left_unlinked;
    
    public PosNodeData(ReteNode node)
    {
        this.from_beta_mem = new ListItem<ReteNode>(node);
    }
    
    private PosNodeData(PosNodeData other)
    {
        this.from_beta_mem = new ListItem<ReteNode>(other.from_beta_mem.item);
        this.node_is_left_unlinked = other.node_is_left_unlinked;
    }
    
    public PosNodeData copy()
    {
        return new PosNodeData(this);
    }
}
