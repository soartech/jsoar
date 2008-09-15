/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 14, 2008
 */
package org.jsoar.kernel;

import java.io.PrintWriter;
import java.io.Writer;

import org.jsoar.kernel.memory.Preference;

/**
 * @author ray
 */
public class Printer
{
    private final PrintWriter writer;
    
    /**
     * @param writer
     */
    public Printer(Writer writer)
    {
        this.writer = new PrintWriter(writer);
    }
    
    public void print(String format, Object ... args)
    {
        this.writer.printf(format, args);
    }

    /**
     * @param pref
     */
    public void print_preference(Preference pref)
    {
        // TODO implement print_preference
        
    }
}
