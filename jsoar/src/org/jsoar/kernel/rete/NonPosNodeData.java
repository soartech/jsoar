/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.util.ListHead;

/**
 * data for all except positive nodes
 * 
 * rete.cpp:394
 * 
 * @author ray
 */
public class NonPosNodeData extends ReteNodeData
{
    ListHead<Token> tokens = new ListHead<Token>(); // dll of tokens at this node
    boolean is_left_unlinked; //:1;           /* used on mp nodes only */
    
    /**
     * @return Shallow copy of this object
     */
    public NonPosNodeData copy()
    {
        NonPosNodeData n = new NonPosNodeData();
        n.tokens = this.tokens;
        n.is_left_unlinked = this.is_left_unlinked;
        return n;
    }

}
