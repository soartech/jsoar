/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on April 4, 2012
 */
package org.jsoar.kernel.epmem;

import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * Standard symbols used by epmem
 * 
 * @author voigtjr
 */
class EpisodicMemorySymbols
{
    public final SymbolImpl epmem_sym;
    public final SymbolImpl epmem_sym_cmd;
    public final SymbolImpl epmem_sym_result;
//    
//    public final Symbol epmem_sym_retrieved;
//    public final Symbol epmem_sym_status;
//    public final Symbol epmem_sym_match_score;
//    public final Symbol epmem_sym_cue_size;
//    public final Symbol epmem_sym_normalized_match_score;
//    public final Symbol epmem_sym_match_cardinality;
//    public final Symbol epmem_sym_memory_id;
//    public final Symbol epmem_sym_present_id;
//    public final Symbol epmem_sym_no_memory;
//    public final Symbol epmem_sym_graph_match;
//    public final Symbol epmem_sym_graph_match_mapping;
//    public final Symbol epmem_sym_graph_match_mapping_node;
//    public final Symbol epmem_sym_graph_match_mapping_cue;
//    public final Symbol epmem_sym_success;
//    public final Symbol epmem_sym_failure;
//    public final Symbol epmem_sym_bad_cmd;
//    
//    public final Symbol epmem_sym_retrieve;
//    public final Symbol epmem_sym_next;
//    public final Symbol epmem_sym_prev;
//    public final Symbol epmem_sym_query;
//    public final Symbol epmem_sym_negquery;
//    public final Symbol epmem_sym_before;
//    public final Symbol epmem_sym_after;
//    public final Symbol epmem_sym_prohibit;

    public EpisodicMemorySymbols(SymbolFactoryImpl syms)
    {
        // symtab.cpp:create_predefined_symbols
        epmem_sym = syms.createString( "smem" );
        epmem_sym_cmd = syms.createString( "command" );
        epmem_sym_result = syms.createString( "result" );
        
        // XXX needs transformation
        
//        thisAgent->epmem_sym = make_sym_constant( thisAgent, "epmem" );
//        thisAgent->epmem_sym_cmd = make_sym_constant( thisAgent, "command" );
//        thisAgent->epmem_sym_result = make_sym_constant( thisAgent, "result" );
//
//        thisAgent->epmem_sym_retrieved = make_sym_constant( thisAgent, "retrieved" );
//        thisAgent->epmem_sym_status = make_sym_constant( thisAgent, "status" );
//        thisAgent->epmem_sym_match_score = make_sym_constant( thisAgent, "match-score" );
//        thisAgent->epmem_sym_cue_size = make_sym_constant( thisAgent, "cue-size" );
//        thisAgent->epmem_sym_normalized_match_score = make_sym_constant( thisAgent, "normalized-match-score" );
//        thisAgent->epmem_sym_match_cardinality = make_sym_constant( thisAgent, "match-cardinality" );
//        thisAgent->epmem_sym_memory_id = make_sym_constant( thisAgent, "memory-id" );
//        thisAgent->epmem_sym_present_id = make_sym_constant( thisAgent, "present-id" );
//        thisAgent->epmem_sym_no_memory = make_sym_constant( thisAgent, "no-memory" );
//        thisAgent->epmem_sym_graph_match = make_sym_constant( thisAgent, "graph-match" );
//        thisAgent->epmem_sym_graph_match_mapping = make_sym_constant( thisAgent, "mapping" );
//        thisAgent->epmem_sym_graph_match_mapping_node = make_sym_constant( thisAgent, "node" );
//        thisAgent->epmem_sym_graph_match_mapping_cue = make_sym_constant( thisAgent, "cue" );
//        thisAgent->epmem_sym_success = make_sym_constant( thisAgent, "success" );
//        thisAgent->epmem_sym_failure = make_sym_constant( thisAgent, "failure" );
//        thisAgent->epmem_sym_bad_cmd = make_sym_constant( thisAgent, "bad-cmd" );
//
//        thisAgent->epmem_sym_retrieve = make_sym_constant( thisAgent, "retrieve" );
//        thisAgent->epmem_sym_next = make_sym_constant( thisAgent, "next" );
//        thisAgent->epmem_sym_prev = make_sym_constant( thisAgent, "previous" );
//        thisAgent->epmem_sym_query = make_sym_constant( thisAgent, "query" );
//        thisAgent->epmem_sym_negquery = make_sym_constant( thisAgent, "neg-query" );
//        thisAgent->epmem_sym_before = make_sym_constant( thisAgent, "before" );
//        thisAgent->epmem_sym_after = make_sym_constant( thisAgent, "after" );
//        thisAgent->epmem_sym_prohibit = make_sym_constant( thisAgent, "prohibit" );

    }
}
