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
    
    private boolean printWarnings = true;
    
    /**
     * @param writer
     */
    public Printer(Writer writer)
    {
        this.writer = new PrintWriter(writer);
    }
    
    public Printer print(String format, Object ... args)
    {
        this.writer.printf(format, args);
        return this;
    }
    
    public Printer startNewLine()
    {
        this.writer.append('\n');
        return this;
    }
    
    public Printer flush()
    {
        this.writer.flush();
        return this;
    }

    /**
     * @param pref
     */
    public void print_preference(Preference pref)
    {
        // TODO implement print_preference
        
    }
    
    public Printer warn(String format, Object ... args)
    {
        if(printWarnings)
        {
            print(format, args);
        }
        return this;
    }

    /**
     * @return the printWarnings
     */
    public boolean isPrintWarnings()
    {
        return printWarnings;
    }

    /**
     * @param printWarnings the printWarnings to set
     */
    public Printer setPrintWarnings(boolean printWarnings)
    {
        this.printWarnings = printWarnings;
        return this;
    }

    /**
     * @param string
     * @param id
     */
    public Printer error(String format, Object ... args)
    {
        print("\nError: " + format, args);
        return this;
    }
    
    
}
