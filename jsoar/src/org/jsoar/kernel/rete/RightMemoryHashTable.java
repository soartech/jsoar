/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class RightMemoryHashTable
{
    private static final int LOG2_RIGHT_HT_SIZE = 14;
    private static final int RIGHT_HT_SIZE = 1 << LOG2_RIGHT_HT_SIZE;

    private static final int RIGHT_HT_MASK = RIGHT_HT_SIZE - 1;
    
    private List<ListHead<RightMemory>> buckets;
    
    public RightMemoryHashTable()
    {
        this.buckets = new ArrayList<ListHead<RightMemory>>(RIGHT_HT_SIZE);
        for(int i = 0; i < RIGHT_HT_SIZE; ++i)
        {
            this.buckets.add(new ListHead<RightMemory>());
        }
    }
    
    public ListHead<RightMemory> right_ht_bucket(int hv) 
    {
        int index = hv & RIGHT_HT_MASK;
        return buckets.get(index);
        //return (* ( ((token **) thisAgent->left_ht) + ((hv) & LEFT_HT_MASK)));
    }
    

}
