/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 15, 2008
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * io_soar.h:170:io_wme
 * 
 * @author ray
 */
public class InputOutputWme
{
    InputOutputWme next;  /* points to next io_wme in the chain */
    Identifier id;                  /* id, attribute, and value of the wme */
    Symbol attr;
    Symbol value;
    int timetag ;       /* DJP: Added.  Only guaranteed valid for an output wme. */
    
    /**
     * @param id
     * @param attr
     * @param value
     * @param timetag
     */
    public InputOutputWme(Identifier id, Symbol attr, Symbol value, int timetag)
    {
        this.id = id;
        this.attr = attr;
        this.value = value;
        this.timetag = timetag;
    }
    
    
}
