/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.util.ListHead;

/**
 * data for beta memory nodes only
 * 
 * rete.cpp:372
 * 
 * @author ray
 */
public class BetaMemoryNodeData extends ReteNodeData
{
    public static String bnode_type_names[/*256*/] =
    {
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","","",   
       "","","","","","","","","","","","","","","",""  
    };

    // first pos node child that is left-linked
    final ListHead<ReteNode> first_linked_child;

    public BetaMemoryNodeData()
    {
        this.first_linked_child = ListHead.newInstance();
    }
    
    public BetaMemoryNodeData(BetaMemoryNodeData other)
    {
        this.first_linked_child = ListHead.newInstance(other.first_linked_child);
    }
    
    /**
     * @return Shallow copy of this object
     */
    public BetaMemoryNodeData copy()
    {
        return new BetaMemoryNodeData(this);
    }  

}
