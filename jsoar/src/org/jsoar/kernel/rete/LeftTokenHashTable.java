/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class LeftTokenHashTable
{
    private static final int LOG2_LEFT_HT_SIZE = 14;
    private static final int LEFT_HT_SIZE = 1 << LOG2_LEFT_HT_SIZE;

    private static final int LEFT_HT_MASK = LEFT_HT_SIZE - 1;
    
    private List<ListHead<LeftToken>> buckets;
    
    public LeftTokenHashTable()
    {
        this.buckets = new ArrayList<ListHead<LeftToken>>(LEFT_HT_SIZE);
        for(int i = 0; i < LEFT_HT_SIZE; ++i)
        {
            this.buckets.add(ListHead.<LeftToken>newInstance());
        }
    }
    
    /**
     * rete.cpp:678:left_ht_bucket
     * 
     * @param hv
     * @return
     */
    public ListHead<LeftToken> left_ht_bucket(int hv) 
    {
        int index = hv & LEFT_HT_MASK;
        return buckets.get(index);
        //return (* ( ((token **) thisAgent->left_ht) + ((hv) & LEFT_HT_MASK)));
    }
    
    /**
     * rete.cpp:694:insert_token_into_left_ht
     * 
     * @param tok
     * @param hv
     */
    public void insert_token_into_left_ht(LeftToken tok, int hv) 
    {
        ListHead<LeftToken> header = buckets.get(hv & LEFT_HT_MASK);
        tok.in_bucket.insertAtHead(header);
//      token **header_zy37;
//      header_zy37 = ((token **) thisAgent->left_ht) + ((hv) & LEFT_HT_MASK);
//      insert_at_head_of_dll (*header_zy37, (tok),
//                             a.ht.next_in_bucket, a.ht.prev_in_bucket);
    }
    
    /**
     * rete.cpp:705:remove_token_from_left_ht
     * 
     * @param tok
     * @param hv
     */
    public void remove_token_from_left_ht(LeftToken tok, int hv)
    {
        ListHead<LeftToken> header = buckets.get(hv & LEFT_HT_MASK);
        tok.in_bucket.remove(header);
//      fast_remove_from_dll (left_ht_bucket(thisAgent, hv), tok, token,
//                            a.ht.next_in_bucket, a.ht.prev_in_bucket);
    }

}
