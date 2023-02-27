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
    
    public final SymbolImpl epmem_sym_retrieved;
    public final SymbolImpl epmem_sym_status;
    public final SymbolImpl epmem_sym_match_score;
    public final SymbolImpl epmem_sym_cue_size;
    public final SymbolImpl epmem_sym_normalized_match_score;
    public final SymbolImpl epmem_sym_match_cardinality;
    public final SymbolImpl epmem_sym_memory_id;
    public final SymbolImpl epmem_sym_present_id;
    public final SymbolImpl epmem_sym_no_memory;
    public final SymbolImpl epmem_sym_graph_match;
    public final SymbolImpl epmem_sym_graph_match_mapping;
    public final SymbolImpl epmem_sym_graph_match_mapping_node;
    public final SymbolImpl epmem_sym_graph_match_mapping_cue;
    public final SymbolImpl epmem_sym_success;
    public final SymbolImpl epmem_sym_failure;
    public final SymbolImpl epmem_sym_bad_cmd;
    
    public final SymbolImpl epmem_sym_retrieve;
    public final SymbolImpl epmem_sym_next;
    public final SymbolImpl epmem_sym_prev;
    public final SymbolImpl epmem_sym_query;
    public final SymbolImpl epmem_sym_negquery;
    public final SymbolImpl epmem_sym_filter;
    public final SymbolImpl epmem_sym_before;
    public final SymbolImpl epmem_sym_after;
    public final SymbolImpl epmem_sym_prohibit;
    public final SymbolImpl epmem_sym_current;
    public final SymbolImpl epmem_sym_store;
    
    public EpisodicMemorySymbols(SymbolFactoryImpl syms)
    {
        // symtab.cpp:create_predefined_symbols
        epmem_sym = syms.createString("epmem");
        epmem_sym_cmd = syms.createString("command");
        epmem_sym_result = syms.createString("result");
        
        epmem_sym_retrieved = syms.createString("retrieved");
        epmem_sym_status = syms.createString("status");
        epmem_sym_match_score = syms.createString("match-score");
        epmem_sym_cue_size = syms.createString("cue-size");
        epmem_sym_normalized_match_score = syms.createString("normalized-match-score");
        epmem_sym_match_cardinality = syms.createString("match-cardinality");
        epmem_sym_memory_id = syms.createString("memory-id");
        epmem_sym_present_id = syms.createString("present-id");
        epmem_sym_no_memory = syms.createString("no-memory");
        epmem_sym_graph_match = syms.createString("graph-match");
        epmem_sym_graph_match_mapping = syms.createString("mapping");
        epmem_sym_graph_match_mapping_node = syms.createString("node");
        epmem_sym_graph_match_mapping_cue = syms.createString("cue");
        epmem_sym_success = syms.createString("success");
        epmem_sym_failure = syms.createString("failure");
        epmem_sym_bad_cmd = syms.createString("bad-cmd");
        
        epmem_sym_retrieve = syms.createString("retrieve");
        epmem_sym_next = syms.createString("next");
        epmem_sym_prev = syms.createString("previous");
        epmem_sym_query = syms.createString("query");
        epmem_sym_negquery = syms.createString("neg-query");
        epmem_sym_filter = syms.createString("filter");
        epmem_sym_before = syms.createString("before");
        epmem_sym_after = syms.createString("after");
        epmem_sym_prohibit = syms.createString("prohibit");
        epmem_sym_current = syms.createString("current");
        epmem_sym_store = syms.createString("store");
        
    }
}
