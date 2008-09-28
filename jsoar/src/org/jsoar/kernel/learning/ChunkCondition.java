/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 27, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.util.AsListItem;
import org.jsoar.util.SoarHashTable;

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
    final AsListItem<ChunkCondition> next_prev = new AsListItem<ChunkCondition>(this);

    /* dll of cond's in this particular hash bucket for this set */
    final AsListItem<ChunkCondition> in_bucket = new AsListItem<ChunkCondition>(this);

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
        while (remainder != 0)
        {
            hv ^= (remainder & SoarHashTable.masks_for_n_low_order_bits[ChunkConditionSet.LOG_2_CHUNK_COND_HASH_TABLE_SIZE]);
            remainder = remainder >> ChunkConditionSet.LOG_2_CHUNK_COND_HASH_TABLE_SIZE;
        }
        cc.compressed_hash_value = hv;
        return cc;
    }
}
