/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;

/** @author voigtjr */
public interface EpisodicMemory {
  /** episodic_memory.cpp:epmem_enabled */
  boolean epmem_enabled();

  /** episodic_memory.cpp:epmem_close */
  void epmem_close() throws SoarException;

  // TODO stub, not sure if these are the correct parameters
  void initializeNewContext(WorkingMemory wm, IdentifierImpl id);

  /**
   * Performs cleanup when a state is removed
   *
   * <p>episodic_memory.h:epmem_reset
   */
  void epmem_reset(IdentifierImpl state);

  /**
   * Default value of {@code allow_store} is {@code true} in C++
   *
   * @see #epmem_go(boolean allow_store)
   */
  void epmem_go();

  /**
   * The kernel calls this function to implement Soar-EpMem: consider new storage and respond to any
   * commands
   *
   * <p>episodic_memory.h:epmem_go
   */
  void epmem_go(boolean allow_store);

  /** Check if new episodes should be processed during the output phase. */
  boolean encodeInOutputPhase();

  /** Check if new episodes should be processed during the selection phase. */
  boolean encodeInSelectionPhase();

  /** Returns the validation count */
  long epmem_validation();

  /** replaces {@code (*thisAgent->epmem_id_ref_counts)[ w->value->id.epmem_id ]->insert( w );} */
  boolean addIdRefCount(long id, WmeImpl w);

  /** replaces {@code thisAgent->epmem_wme_adds->insert( w->id );} */
  void addWme(IdentifierImpl id);

  /** replaces {@code _epmem_remove_wme( agent* my_agent, wme* w )} in rete */
  void removeWme(WmeImpl w);

  /** replaces {@code _epmem_process_ids( agent* my_agent )} in rete.cpp : 1691 */
  void processIds();
}
