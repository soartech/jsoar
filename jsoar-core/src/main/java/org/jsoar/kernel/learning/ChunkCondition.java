/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 27, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.util.ListItem;
import org.jsoar.util.HashTable;

/**
 * chunk.h:27:chunk_cond
 * 
 * @author ray
 */
public class ChunkCondition
{
    final Condition cond;                /* points to the original condition */

    Condition instantiated_cond;   /* points to cond in chunk instantiation */
    Condition variablized_cond;    /* points to cond in the actual chunk */
    Condition saved_prev_pointer_of_variablized_cond; /* don't ask */

    /* dll of all cond's in a set (i.e., a chunk_cond_set, or the grounds) */
    final ListItem<ChunkCondition> next_prev = new ListItem<ChunkCondition>(this);

    /* dll of cond's in this particular hash bucket for this set */
    final ListItem<ChunkCondition> in_bucket = new ListItem<ChunkCondition>(this);

    int hash_value;             /* equals hash_condition(cond) */
    int compressed_hash_value;  /* above, compressed to a few bits */
    
    ChunkCondition(Condition cond)
    {
        this.cond = cond;
    }

    /**
     * chunk.cpp:338:make_chunk_cond_for_condition
     * 
     * @param cond
     * @return
     */
    static ChunkCondition make_chunk_cond_for_condition(Condition cond)
    {
        ChunkCondition cc = new ChunkCondition(cond);
        cc.hash_value = Condition.hash_condition(cond);
        int remainder = cc.hash_value;
        int hv = 0;
        // Have to test for -1 here as well because Java doesn't have unsigned ints
        // which means that the shift below can force it to 0xffffffff, i.e. -1 which
        // will never get to 0.
        while (remainder != 0 && remainder != -1)
        {
            final int masked = remainder & HashTable.masks_for_n_low_order_bits[ChunkConditionSet.LOG_2_CHUNK_COND_HASH_TABLE_SIZE];
            hv ^= (masked);
            remainder = remainder >> ChunkConditionSet.LOG_2_CHUNK_COND_HASH_TABLE_SIZE;
        }
        cc.compressed_hash_value = hv;
        return cc;
    }
}
