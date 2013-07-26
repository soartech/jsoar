/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * <p>semantic_memory.h:323:smem_chunk_struct
 * <p>semantic_memory.h:340:smem_chunk_value_lti
 * <p>semantic_memory.h:334:smem_chunk_value_constant - This was eliminated
 * and replaced with just a raw SymbolImpl. See {@link #slots} below.
 * 
 * @author ray
 */
class smem_chunk_lti
{
    IdentifierImpl soar_id;
    /*smem_lti_id*/ long lti_id;

    char lti_letter;
    /*uint64_t*/ long lti_number;

    /**
     * Map from attributes to slot values. The list is comprised of
     * {@link SymbolImpl} for constants and {@link smem_chunk_lti} for
     * identifiers. This mechanism was chosen to reduce memory usage.
     * 
     * <p>Original type: smem_slot_map
     */
    Map<SymbolImpl, List<Object>> slots;
    
    /**
     * Construct a new slot map. Use this rather than creating a raw HashMap. It looks
     * nicer and we can tune the map in one place that way.
     * 
     * @return a new slot map
     */
    static Map<SymbolImpl, List<Object /*smem_chunk_lti or SymbolImpl*/>> newSlotMap()
    {
        // TODO SMEM is this a good idea?
        // use a smaller default size since there usually aren't that many
        // attributes on an id 
        return new LinkedHashMap<SymbolImpl, List<Object>>(8);
    }
    
    /**
     * Create a new slot list for the given attribute in the given slot map.
     * 
     * <p>semantic_memory.cpp:1116:smem_make_slot
     * 
     * @param slots the slot map
     * @param attr the attribute
     * @return the new list.
     */
    static List<Object> smem_make_slot( Map<SymbolImpl, List<Object>> slots, SymbolImpl attr )
    {
        List<Object> s = slots.get(attr);
        if(s == null)
        {
            // Use a smaller default size since there usually aren't that many
            // multi-attributes on an id.
            s = new ArrayList<Object>(4);
            slots.put(attr, s);
        }
        return s;
    }
    
    
}
