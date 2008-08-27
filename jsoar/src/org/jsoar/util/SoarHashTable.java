/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 24, 2008
 */
package org.jsoar.util;


/**
 * mem.h
 * 
 * @author ray
 */
public class SoarHashTable <T extends ItemInHashTable>
{
    /**
     * mem.cpp:487
     */
    public static final int masks_for_n_low_order_bits[] = { 0x00000000,
            0x00000001, 0x00000003, 0x00000007, 0x0000000F,
            0x0000001F, 0x0000003F, 0x0000007F, 0x000000FF,
            0x000001FF, 0x000003FF, 0x000007FF, 0x00000FFF,
            0x00001FFF, 0x00003FFF, 0x00007FFF, 0x0000FFFF,
            0x0001FFFF, 0x0003FFFF, 0x0007FFFF, 0x000FFFFF,
            0x001FFFFF, 0x003FFFFF, 0x007FFFFF, 0x00FFFFFF,
            0x01FFFFFF, 0x03FFFFFF, 0x07FFFFFF, 0x0FFFFFFF,
            0x1FFFFFFF, 0x3FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF };
    
    private int count;      /* number of items in the table */
    private int size;       /* number of buckets */
    private int log2size;           /* log (base 2) of size */
    private int minimum_log2size;   /* table never shrinks below this size */
    private ItemInHashTable[] buckets;
    private HashFunction<T> h;          // call this to hash or rehash an item

    /**
     * mem.cpp:497
     * 
     * @param minimum_log2size
     * @param h
     */
    public SoarHashTable(int minimum_log2size, HashFunction<T> h)
    {
        this.count = 0;
        this.minimum_log2size = minimum_log2size < 1 ? 1 : minimum_log2size;
        this.size = 1 << this.minimum_log2size;
        this.log2size = minimum_log2size;
        this.buckets = allocateBuckets(this.size);
        this.h = h;
    }
    
    public int getLog2Size()
    {
        return log2size;
    }
    
    @SuppressWarnings("unchecked")
    public T getBucket(int hv)
    {
        // TODO: Should there be a mask here, like this: hv = hv & masks_for_n_low_order_bits[getLog2Size()];
        // It seems like this is always done externally in the kernel code.

        return (T) buckets[hv];
    }
    
    /**
     * mem.cpp:548
     * 
     * @param item
     */
    public void remove_from_hash_table(T item)
    {
        ItemInHashTable this_one = item;

        int hash_value = h.calculate(item, log2size);
        if (buckets[hash_value] == this_one)
        {
            /* --- hs is the first one on the list for the bucket --- */
            buckets[hash_value] = this_one.next_in_hash_table;
        }
        else
        {
            /*
             * --- hs is not the first one on the list, so find its predecessor
             * ---
             */
            ItemInHashTable prev = buckets[hash_value];
            while (prev != null && prev.next_in_hash_table != this_one)
            {
                prev = prev.next_in_hash_table;
            }
            if (prev == null)
            {
                /* Reaching here means that we couldn't find this_one item */
                // TODO: assert(prev && "Couldn't find item to remove from hash
                // table!");
                return;
            }
            prev.next_in_hash_table = this_one.next_in_hash_table;
        }
        this_one.next_in_hash_table = null; /* just for safety */
        /* --- update count and possibly resize the table --- */
        this.count--;
        if ((this.count < this.size / 2) && (this.log2size > this.minimum_log2size))
        {
            resize_hash_table((int) (this.log2size - 1));
        }

    }
    
    /**
     * mem.cpp:576
     * 
     * @param item
     */
    public void add_to_hash_table(T item)
    {
        ItemInHashTable this_one = item;
        this.count++;
        if (this.count >= this.size * 2)
        {
            resize_hash_table((int) (this.log2size + 1));
        }
        int hash_value = h.calculate(item, this.log2size);
        this_one.next_in_hash_table = buckets[hash_value];
        buckets[hash_value] = this_one;
    }

//typedef Bool (*hash_table_callback_fn)(void *item);
//typedef Bool (*hash_table_callback_fn2)(agent* thisAgent, void *item, FILE* f);
//
//extern void do_for_all_items_in_hash_table (agent* thisAgent, struct hash_table_struct *ht,
//      hash_table_callback_fn2 f, FILE* fn);
//extern void do_for_all_items_in_hash_bucket (struct hash_table_struct *ht,
//       hash_table_callback_fn f,
//       unsigned long hash_value);

    private static ItemInHashTable[] allocateBuckets(int size)
    {
        return new ItemInHashTable[size];
    }
    
    /**
     * mem.cpp:514
     * 
     * @param new_log2size
     */
    @SuppressWarnings("unchecked")
    private void resize_hash_table(int new_log2size)
    {
        int new_size = 1 << new_log2size;
        ItemInHashTable[] new_buckets = allocateBuckets(new_size);

        ItemInHashTable next = null;
        for (int i = 0; i < size; i++)
        {
            for (ItemInHashTable item = buckets[i]; item != null; item = next)
            {
                next = item.next_in_hash_table;
                /* --- insert item into new buckets --- */
                int hash_value = h.calculate((T) item, new_log2size);
                item.next_in_hash_table = new_buckets[hash_value];
                new_buckets[hash_value] = item;
            }
        }

        this.buckets = new_buckets;
        this.size = new_size;
        this.log2size = new_log2size;
    }
    
}
