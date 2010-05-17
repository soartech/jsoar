/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.WmeImpl;

/**
 * rete.cpp:195
 * 
 * @author ray
 */
public class RightMemory
{
    final WmeImpl w;                      /* the wme */
    final AlphaMemory am;               /* the alpha memory */
    
    
    //final ListItem<RightMemory> in_bucket = new ListItem<RightMemory>(this); // hash bucket dll
    RightMemory next_in_bucket, prev_in_bucket; /*hash bucket dll*/
    
    //final ListItem<RightMemory> in_am = new ListItem<RightMemory>(this); // rm's in this amem
    RightMemory next_in_am, prev_in_am;       /*rm's in this amem*/
    
    //public final AsListItem<RightMemory> from_wme = new AsListItem<RightMemory>(this); // tree-based remove
    private RightMemory next_from_wme, prev_from_wme; /*tree-based remove*/

    /**
     * @param w
     * @param am
     */
    public RightMemory(WmeImpl w, AlphaMemory am)
    {
        this.w = w;
        this.am = am;
    }

    public RightMemory addToWme(RightMemory head)
    {
        next_from_wme = head;
        prev_from_wme = null;
        if(head != null)
        {
            head.prev_from_wme = this;
        }
        return this;
    }
    
    public RightMemory removeFromWme(RightMemory head)
    {
        if(next_from_wme != null)
        {
            next_from_wme.prev_from_wme = prev_from_wme;
        }
        if(prev_from_wme != null)
        {
            prev_from_wme.next_from_wme = next_from_wme;
        }
        else
        {
            head = next_from_wme;
        }
        next_from_wme = null;
        prev_from_wme = null;
        
        return head;
        
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "am:" + am + "/w:" + w;
    }
}
