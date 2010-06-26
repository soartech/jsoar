/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.symbols.IdentifierImpl;

/**
 * @author ray
 */
public interface SemanticMemory
{
    /**
     * semantic_memory.h:239:SMEM_LTI_UNKNOWN_LEVEL
     */
    public static final int LTI_UNKNOWN_LEVEL = 0;
    
    /**
     * semantic_memory.h:SMEM_LTI_UNKNOWN_LEVEL
     */
    boolean smem_enabled();
    
    /**
     * semantic_memory.h:SMEM_LTI_UNKNOWN_LEVEL
     * @throws SoarException 
     */
    void smem_attach() throws SoarException;
    
    /**
     * make sure ltis in actions are grounded
     * 
     * <p>semantic_memory.h:smem_valid_production
     */
    boolean smem_valid_production(Condition lhs_top, Action rhs_top);
    
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
     * Performs cleanup when a state is removed
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
}
