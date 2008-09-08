/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.util.AsListItem;

/**
 * rete.cpp:195
 * 
 * @author ray
 */
public class RightMemory
{
    final Wme w;                      /* the wme */
    final AlphaMemory am;               /* the alpha memory */
    
    
    final AsListItem<RightMemory> in_bucket = new AsListItem<RightMemory>(this); // hash bucket dll
    //RightMemory next_in_bucket, prev_in_bucket; /*hash bucket dll*/
    
    final AsListItem<RightMemory> in_am = new AsListItem<RightMemory>(this); // rm's in this amem
    //RightMemory next_in_am, prev_in_am;       /*rm's in this amem*/
    
    final AsListItem<RightMemory> from_wme = new AsListItem<RightMemory>(this); // tree-based remove
    //RightMemory next_from_wme, prev_from_wme; /*tree-based remove*/

    /**
     * @param w
     * @param am
     */
    public RightMemory(Wme w, AlphaMemory am)
    {
        this.w = w;
        this.am = am;
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
