/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;

/**
 * semantic_memory.h:323:smem_chunk_struct
 * semantic_memory.h:340:smem_chunk_value_lti
 * 
 * @author ray
 */
public class smem_chunk_lti
{
    IdentifierImpl soar_id;
    /*smem_lti_id*/ long lti_id;

    char lti_letter;
    /*uint64_t*/ long lti_number;

    /*smem_slot_map*/ final Map<Symbol, List<smem_chunk_value>> slots = new HashMap<Symbol, List<smem_chunk_value>>();
}
