/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Wme;
import org.jsoar.util.AsListItem;

/**
 * rete.cpp:195
 * 
 * @author ray
 */
public class RightMemory
{
    Wme w;                      /* the wme */
    AlphaMemory am;               /* the alpha memory */
    
    AsListItem<RightMemory> in_bucket = new AsListItem<RightMemory>(this); // hash bucket dll
    //RightMemory next_in_bucket, prev_in_bucket; /*hash bucket dll*/
    
    AsListItem<RightMemory> in_am = new AsListItem<RightMemory>(this); // rm's in this amem
    //RightMemory next_in_am, prev_in_am;       /*rm's in this amem*/
    
    AsListItem<RightMemory> from_wme = new AsListItem<RightMemory>(this); // tree-based remove
    //RightMemory next_from_wme, prev_from_wme; /*tree-based remove*/

}
