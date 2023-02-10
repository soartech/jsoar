/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.util.PriorityQueue;

import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.smem.math.MathQuery;

/**
 * <p>semantic_memory.h:279:smem_weighted_cue_element
 * 
 * @author ray
 */
public class WeightedCueElement implements Comparable<WeightedCueElement>
{
    public static PriorityQueue<WeightedCueElement> newPriorityQueue()
    {
        return new PriorityQueue<WeightedCueElement>();
    }
    
    /* uint64_t */ long weight;
    
    WmeImpl cue_element;
    /* smem_hash_id */ long attr_hash;
    /* smem_hash_id */ long value_hash;
    /* smem_hash_id */ long value_lti;
    
    smem_cue_element_type element_type;
    boolean pos_element;
    public MathQuery mathElement = null;
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(WeightedCueElement o)
    {
        // semantic_memory.h:292:smem_compare_weighted_cue_elements
        return (weight > o.weight ? 1 : (weight < o.weight ? -1 : 0));
    }
}
