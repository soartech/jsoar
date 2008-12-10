/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 14, 2008
 */
package org.jsoar.kernel.tracing;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.apache.commons.io.output.NullWriter;

/**
 * Soar agent print interface
 * 
 * @author ray
 */
public class Printer
{
    private static final char[] SPACES = new char[80];
    static
    {
        Arrays.fill(SPACES, ' ');
    }
    
    private Writer internalWriter;
    private PrintWriter wrappedWriter;
    
    private boolean printWarnings = true;
    
    private final LinkedList<StackEntry> stack = new LinkedList<StackEntry>();
    
    /**
     * @return a default printer that prints to standard output
     */
    public static Printer createStdOutPrinter()
    {
        return new Printer(new OutputStreamWriter(System.out), true);
    }
    
    /**
     * Construct a new printer that prints to the given writer
     * 
     * @param writer The writer to write to. If null, then a NullWriter is used
     *          and all output will be dropped.
     * @param autoFlush
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
     * Push a new writer to print to. The current writer is flushed and then
     * pushed onto a stack. It can be restored with {@link #popWriter()}
     * 
     * @param writer The new writer to write to. If null, the a NullWriter is
     *      used and all output will be dropped.
     * @param autoFlush If true, writer will autoflush on prints
     */
    public void pushWriter(Writer writer, boolean autoFlush)
    {
        wrappedWriter.flush();
        stack.push(new StackEntry(internalWriter, wrappedWriter));
        
        this.internalWriter = writer != null ? writer : new NullWriter();
        this.wrappedWriter = new PrintWriter(internalWriter, autoFlush);
    }
    
    /**
     * Pop the current writer from the writer stack. The current writer is
     * flushed and returned. The writer currently on the top of the stack
     * becomes the new writer.
     * 
     * @return The old writer
     * @throws NoSuchElementException if the writer stack is empty
     */
    public Writer popWriter()
    {
        wrappedWriter.flush();
        
        final Writer oldInternal = this.internalWriter;
        
        StackEntry e = stack.pop();
        this.internalWriter = e.internal;
        this.wrappedWriter = e.wrapped;
        
        return oldInternal;
    }
    
    public Printer print(String output)
    {
        this.wrappedWriter.print(output);
        return this;
    }
    
    public Printer print(String format, Object ... args)
    {
        this.wrappedWriter.printf(format, args);
        return this;
    }
    
    public Formatter asFormatter()
    {
        return new Formatter(this.wrappedWriter);
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

    public Printer warn(String format, Object ... args)
    {
        if(printWarnings)
        {
            print(format, args);
        }
        return this;
    }

    /**
     * <p>gsysparam.h:132:PRINT_WARNINGS_SYSPARAM
     * 
     * @return the printWarnings
     */
    public boolean isPrintWarnings()
    {
        return printWarnings;
    }

    /**
     * <p>gsysparam.h:132:PRINT_WARNINGS_SYSPARAM
     * 
     * @param printWarnings the printWarnings to set
     * @return this
     */
    public Printer setPrintWarnings(boolean printWarnings)
    {
        this.printWarnings = printWarnings;
        return this;
    }

    /**
     * Print an error
     * 
     * @param format format string
     * @param args arguments
     * @return this
     */
    public Printer error(String format, Object ... args)
    {
        print("\nError: " + format, args);
        return this;
    }

    /**
     * <p>COLUMNS_PER_LINE
     * 
     * @return columns per line in this printer
     */
    public int getColumnsPerLine()
    {
        return 80;
    }

    /**
     * @return the current output column of the printer
     */
    public int getOutputColumn()
    {
        // TODO implement get_printer_output_column
        return 0;
    }
    
    /**
     * Append n spaces to the printer
     * 
     * @param n Number of spaces
     * @return this
     */
    public Printer spaces(int n)
    {
        while(n > 0)
        {
            int c = Math.min(n, SPACES.length);
            wrappedWriter.write(SPACES, 0, c);
            n -= c;
        }
        return this;
    }
    
    private static class StackEntry
    {
        final Writer internal;
        final PrintWriter wrapped;
        
        /**
         * @param internal
         * @param wrapped
         */
        public StackEntry(Writer internal, PrintWriter wrapped)
        {
            this.internal = internal;
            this.wrapped = wrapped;
        }
    }
}
