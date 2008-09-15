/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * io.cpp
 * 
 * @author ray
 */
public class InputOutput
{

    public boolean output_link_changed;

    /**
     * @param id
     * @param attr
     * @param value
     */
    public void add_input_wme(Identifier id, Symbol attr, Symbol value)
    {
        // TODO implement add_input_wme
        throw new UnsupportedOperationException("add_input_wme not implemented");
    }

    /**
     * 
     */
    public void do_input_cycle()
    {
        // TODO implement do_input_cycle
        throw new UnsupportedOperationException("do_input_cycle not implemented");
    }

    /**
     * 
     */
    public void do_output_cycle()
    {
        // TODO implement do_output_cycle
        throw new UnsupportedOperationException("do_output_cycle not implemented");
    }

}
