/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.LongTermIdentifierSource;

/**
 * @author ray
 */
public interface SemanticMemory extends LongTermIdentifierSource
{
    /**
     * semantic_memory.h:smem_enabled
     */
    boolean smem_enabled();
    
    /**
     * semantic_memory.h:SMEM_LTI_UNKNOWN_LEVEL
     * @throws SoarException 
     */
    void smem_attach() throws SoarException;
    
    /**
     * gets the lti id for an existing lti letter/number pair (or NIL if failure)
     * 
     * <p>semantic_memory.h:smem_lti_get_id
     * @throws SoarException 
     */
    long /*smem_lti_id*/ smem_lti_get_id(char name_letter, long name_number) throws SoarException;
    
    /**
     * returns a reference to an lti
     * 
     * <p>semantic_memory.h:smem_lti_soar_make
     */
    IdentifierImpl smem_lti_soar_make(/*smem_lti_id*/ long lti, char name_letter, long name_number, /*goal_stack_level*/ int level);
    
    /**
     * Performs cleanup when a state is removed.
     * 
     * <p>semantic_memory.h:smem_reset
     */
    void smem_reset(IdentifierImpl state);
    
    /**
     * semantic_memory.h:smem_reset_id_counters
     * @throws SoarException 
     */
    void smem_reset_id_counters() throws SoarException;
    
    /**
     * Performs cleanup operations when the database needs to be closed (end soar, manual close, etc)
     * 
     * semantic_memory.h:smem_close
     * @throws SoarException 
     */
    void smem_close() throws SoarException;
    
    /**
     * semantic_memory.h:smem_go
     */
    void smem_go(boolean store_only);
    
    /**
     * smem_stats->reset()
     */
    void resetStatistics();
    
    /**
     * Returns an object that exposes statistics about semantic memory
     */
    SemanticMemoryStatistics getStatistics();
    
    /**
     * Attaches smem_info to the given identifier. This code is factored out of
     * decide.cpp:create_new_context()
     * 
     * @param id the new context to initialize
     */
    void initializeNewContext(WorkingMemory wm, IdentifierImpl id);
}
