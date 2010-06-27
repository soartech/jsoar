/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.util.List;
import java.util.Map;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * semantic_memory.h:323:smem_chunk_struct
 * semantic_memory.h:340:smem_chunk_value_lti
 * 
 * @author ray
 */
public class smem_chunk_lti implements smem_chunk_value
{
    IdentifierImpl soar_id;
    /*smem_lti_id*/ long lti_id;

    char lti_letter;
    /*uint64_t*/ long lti_number;

    /*smem_slot_map*/ Map<SymbolImpl, List<smem_chunk_value>> slots;

    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.smem_chunk_value#asConstant()
     */
    @Override
    public SymbolImpl asConstant()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.smem_chunk_value#asLti()
     */
    @Override
    public smem_chunk_lti asLti()
    {
        return this;
    }
    
    
}
