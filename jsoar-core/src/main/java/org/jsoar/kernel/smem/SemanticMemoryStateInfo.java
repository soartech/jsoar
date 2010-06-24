/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.IdentifierImpl;

/**
 * SMem info associated with a state. This structure isn't accessed in a tight loop
 * so we can just maintain a map in {@link SemanticMemory}
 * 
 * semantic_memory.h:266:smem_data_struct
 * decide.cpp:2273:create_new_context (initialization)
 * 
 * @author ray
 */
public class SemanticMemoryStateInfo
{
    public final long last_cmd_time[] = new long[2];         // last update to smem.command
    public final long last_cmd_count[] = new long[2];        // last update to smem.command

    public final Set<WmeImpl> cue_wmes = new HashSet<WmeImpl>();                     // wmes in last cue
    public final Deque<Preference> smem_wmes = new ArrayDeque<Preference>(); // wmes in last smem
    
    public final IdentifierImpl smem_header = null; 
    public final IdentifierImpl smem_cmd_header = null;
    public final IdentifierImpl smem_result_header = null;
}
