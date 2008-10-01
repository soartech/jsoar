/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 27, 2008
 */
package org.jsoar.kernel.learning;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * chunk.h:45:chunk_cond_set
 * 
 * @author ray
 */
public class ChunkConditionSet
{
    static int CHUNK_COND_HASH_TABLE_SIZE = 1024;
    static int LOG_2_CHUNK_COND_HASH_TABLE_SIZE = 10;
    
    /**
     * header for dll of all chunk_cond's in the set
     */
    final ListHead<ChunkCondition> all = ListHead.newInstance();
    /**
     * hash table buckets. Defaults to null, so no need to initialize
     */
    final List<ListHead<ChunkCondition>> table = new ArrayList<ListHead<ChunkCondition>>(CHUNK_COND_HASH_TABLE_SIZE);
    {
        for(int i = 0; i < CHUNK_COND_HASH_TABLE_SIZE; ++i)
        {
            table.add(ListHead.<ChunkCondition>newInstance());
        }
    }

    /**
     * 
     * <p>chunk.cpp:356:add_to_chunk_cond_set
     * 
     * @param new_cc
     * @return
     */
    boolean add_to_chunk_cond_set(ChunkCondition new_cc)
    {
        AsListItem<ChunkCondition> old;
        final ListHead<ChunkCondition> bucket = this.table.get(new_cc.compressed_hash_value);
        for (old = bucket.first; old != null; old = old.next)
            if (old.item.hash_value == new_cc.hash_value)
                if (Condition.conditions_are_equal(old.item.cond, new_cc.cond))
                    break;
        
        if (old != null)
        {
            // the new condition was already in the set; so don't add it
            return false;
        }
        // add new_cc to the table
        new_cc.next_prev.insertAtHead(all);
        new_cc.in_bucket.insertAtHead(bucket);
        return true;
    }

    /**
     * 
     * <p>chunk.cpp:376:remove_from_chunk_cond_set
     * 
     * @param cc
     */
    void remove_from_chunk_cond_set(ChunkCondition cc)
    {
        cc.next_prev.remove(all);
        cc.in_bucket.remove(table.get(cc.compressed_hash_value));
    }
}
