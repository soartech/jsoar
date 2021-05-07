package org.jsoar.kernel.epmem;

import java.util.LinkedList;

/**
 * epmem_id_reservation_struct episodic_memory.h:468
 *
 * @author skrawczyk
 */
public class EpisodicMemoryIdReservation {
  public long /*epmem_node_id*/ my_id;
  public final long /*epmem_hash_id*/ my_hash;
  public LinkedList<EpisodicMemoryIdPair> my_pool;

  public EpisodicMemoryIdReservation(long id, long hash) {
    my_id = id;
    my_hash = hash;
  }

  public static class EpisodicMemoryIdPair {
    /** the epmem_id of a wme value which is an identifier */
    public final long first;
    /** the epmem_id of a wme */
    public final long second;

    /**
     * @param wmeValueIdentifierEpmemId - the epmem_id of a wme value which is an identifier
     * @param wmeEpmemId - the epmem_id of a wme
     */
    public EpisodicMemoryIdPair(long wmeValueIdentifierEpmemId, long wmeEpmemId) {
      this.first = wmeValueIdentifierEpmemId;
      this.second = wmeEpmemId;
    }
  }
}
