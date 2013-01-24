package org.jsoar.kernel.epmem;

import java.util.Map;

/**
 * epmem_id_reservation_struct
 * episodic_memory.h:468
 * 
 * @author skrawczyk
 *
 */

public class EpisodicMemoryIdReservation
{
    public long /*epmem_node_id*/ my_id;
    public final long /*epmem_hash_id*/ my_hash;
    
    /**
     * Map is not initialized.
     */
    public Map<Long, Long> /*epmem_id_pool*/ my_pool;
    
    public EpisodicMemoryIdReservation(long id, long hash)
    {
        my_id = id;
        my_hash = hash;
    }

    // CK: changing this to Map<Long, Long>
//    public static class EpisodicMemoryIdPair
//    {
//        public final long first;
//        public final long second;
//        
//        public EpisodicMemoryIdPair(long first, long second)
//        {
//            this.first = first;
//            this.second = second;
//        }
//    }
}
