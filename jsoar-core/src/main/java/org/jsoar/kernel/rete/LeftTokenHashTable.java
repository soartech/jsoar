/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;


/**
 * @author ray
 */
public class LeftTokenHashTable
{
    private static final int LOG2_LEFT_HT_SIZE = 14;
    private static final int LEFT_HT_SIZE = 1 << LOG2_LEFT_HT_SIZE;

    private static final int LEFT_HT_MASK = LEFT_HT_SIZE - 1;
    
    private final LeftToken[] buckets = new LeftToken[LEFT_HT_SIZE];
    
    public LeftTokenHashTable()
    {
    }
    
    /**
     * Get the head of the bucket for a particular hash code. Iterate over this
     * list using {@link LeftToken#next_in_bucket}.
     * 
     * <p>rete.cpp:678:left_ht_bucket
     * 
     * @param hv
     * @return the head of the bucket
     */
    public LeftToken left_ht_bucket(int hv) 
    {
        int index = hv & LEFT_HT_MASK;
        return buckets[index];
        //return (* ( ((token **) thisAgent->left_ht) + ((hv) & LEFT_HT_MASK)));
    }
    
    /**
     * <p>rete.cpp:694:insert_token_into_left_ht
     * 
     * @param tok
     * @param hv
     */
    public void insert_token_into_left_ht(LeftToken tok, int hv) 
    {
        buckets[hv & LEFT_HT_MASK] = tok.addToHashTable(buckets[hv & LEFT_HT_MASK]);
    }
    
    /**
     * rete.cpp:705:remove_token_from_left_ht
     * 
     * @param tok
     * @param hv
     */
    public void remove_token_from_left_ht(LeftToken tok, int hv)
    {
        buckets[hv & LEFT_HT_MASK] = tok.removeFromHashTable(buckets[hv & LEFT_HT_MASK]);
    }

}
