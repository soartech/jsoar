/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

/**
 * @author ray
 */
public class RightMemoryHashTable
{
    private static final int LOG2_RIGHT_HT_SIZE = 14;
    private static final int RIGHT_HT_SIZE = 1 << LOG2_RIGHT_HT_SIZE;
    
    private static final int RIGHT_HT_MASK = RIGHT_HT_SIZE - 1;
    
    private final RightMemory buckets[] = new RightMemory[RIGHT_HT_SIZE];
    
    /**
     * <p>rete.cpp:683:right_ht_bucket
     * 
     * @param hv
     * @return the head of the right memory bucket
     */
    RightMemory right_ht_bucket(int hv)
    {
        final int index = hv & RIGHT_HT_MASK;
        return buckets[index];
        // return (* ( ((token **) thisAgent->left_ht) + ((hv) & LEFT_HT_MASK)));
    }
    
    void insertAtHeadOfBucket(int hv, RightMemory rm)
    {
        final int index = hv & RIGHT_HT_MASK;
        final RightMemory oldHead = buckets[index];
        if(oldHead == null)
        {
            rm.next_in_bucket = null;
        }
        else
        {
            rm.next_in_bucket = oldHead;
            oldHead.prev_in_bucket = rm;
        }
        rm.prev_in_bucket = null;
        buckets[index] = rm;
    }
    
    void removeFromBucket(int hv, RightMemory rm)
    {
        final int index = hv & RIGHT_HT_MASK;
        if(rm == buckets[index])
        {
            if(rm.next_in_bucket != null)
            {
                rm.next_in_bucket.prev_in_bucket = null;
            }
            buckets[index] = rm.next_in_bucket;
        }
        else
        {
            rm.prev_in_bucket.next_in_bucket = rm.next_in_bucket;
            if(rm.next_in_bucket != null)
            {
                rm.next_in_bucket.prev_in_bucket = rm.prev_in_bucket;
            }
        }
        rm.prev_in_bucket = null;
        rm.next_in_bucket = null;
    }
}
