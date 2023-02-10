/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 22, 2008
 */
package org.jsoar.kernel.tracing;

import java.util.List;

import org.jsoar.kernel.symbols.Symbol;

/**
 * trace.cpp:72:trace_format_struct
 * 
 * @author ray
 */
public class TraceFormat
{
    TraceFormat next; /* next in linked list of format items */
    TraceFormatType type; /* what kind of item this is */
    int num; /* for formats with extra numeric arg */
    
    // data depending on trace format type
    
    // union trace_format_data_union {
    String data_string; /* string to print */
    TraceFormat data_subformat; /* [subformat in brackets] */
    List<Symbol> data_attribute_path; /* list.of.attr.path.symbols (NIL if path is '*') */
    // } data;
    
}
