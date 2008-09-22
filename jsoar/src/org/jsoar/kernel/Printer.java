/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 14, 2008
 */
package org.jsoar.kernel;

import java.io.PrintWriter;
import java.io.Writer;

import org.apache.commons.io.output.NullWriter;
import org.jsoar.kernel.memory.Preference;

/**
 * @author ray
 */
public class Printer
{
    private Writer internalWriter;
    private PrintWriter wrappedWriter;
    
    private boolean printWarnings = true;
    
    /**
     * @param writer The writer to write to. If null, then a NullWriter is used
     *          and all output will be dropped.
     */
    public Printer(Writer writer, boolean autoFlush)
    {
        this.internalWriter = writer != null ? writer : new NullWriter();
        this.wrappedWriter = new PrintWriter(internalWriter, autoFlush);
    }
    
    /**
     * @return The current writer
     */
    public Writer getWriter()
    {
        return internalWriter;
    }
    
    /**
     * Set the current writer to print to.
     * 
     * @param writer The new writer to write to. If null, the a NullWriter is
     *      used and all output will be dropped.
     * @param autoFlush If true, writer will autoflush on prints
     */
    public void setWriter(Writer writer, boolean autoFlush)
    {
        this.internalWriter = writer != null ? writer : new NullWriter();
        this.wrappedWriter = new PrintWriter(internalWriter, autoFlush);
    }
    
    public Printer print(String format, Object ... args)
    {
        this.wrappedWriter.printf(format, args);
        return this;
    }
    
    public Printer startNewLine()
    {
        this.wrappedWriter.append('\n');
        return this;
    }
    
    public Printer flush()
    {
        this.wrappedWriter.flush();
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
