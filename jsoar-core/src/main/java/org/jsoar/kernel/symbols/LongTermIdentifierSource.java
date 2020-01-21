/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2010
 */
package org.jsoar.kernel.symbols;

import org.jsoar.kernel.SoarException;

/**
 * Interface for looking up LTIs.
 * 
 * @author ray
 */
public interface LongTermIdentifierSource
{
    /**
     * semantic_memory.h:239:SMEM_LTI_UNKNOWN_LEVEL
     */
    public static final int LTI_UNKNOWN_LEVEL = 0;
    
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
}
