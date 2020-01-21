/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 14, 2008
 */
package org.jsoar.kernel.tracing;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.jsoar.util.NullWriter;
import org.jsoar.util.TeeWriter;

/**
 * Soar agent print interface
 * 
 * <p>The following symbols were removed:
 * <ul>
 * <li>get_printer_output_column
 * <li>COLUMNS_PER_LINE
 * </ul>
 * @author ray
 */
public class Printer
{
    private static final char[] SPACES = new char[80];
    static
    {
        Arrays.fill(SPACES, ' ');
    }
    
    private TeeWriter persistentWriters = new TeeWriter();
    private PrintWriter persistentPrintWriter = new PrintWriter(persistentWriters, true);
    
    private Writer internalWriter;
    private PrintWriter printWriter;
    
    private boolean printWarnings = true;
    private boolean atStartOfLine = true;
    
    private TeeWriter teeWriter = null;
    
    private final Writer startOfLineDetector = new Writer() {

        @Override
        public void close() throws IOException { }

        @Override
        public void flush() throws IOException { }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException
        {
            for(int i = off; i < len; ++i)
            {
                atStartOfLine = cbuf[i] == '\n';
            }
        }
    };
    
    private final LinkedList<StackEntry> stack = new LinkedList<StackEntry>();

    private List<String> warnings;
    
    /**
     * @return a default printer that prints to standard output
     */
    public static Printer createStdOutPrinter()
    {
        return new Printer(new OutputStreamWriter(System.out));
    }
    
    /**
     * Construct a new printer that prints to the given writer
     * 
     * @param writer The writer to write to. If null, then a NullWriter is used
     *          and all output will be dropped.
     */
    public Printer(Writer writer)
    {
        this.internalWriter = writer != null ? writer : new NullWriter();
        this.printWriter = new PrintWriter(internalWriter, true);
        
        addPersistentWriter(startOfLineDetector);

        warnings = new ArrayList<>();
    }
    
    /**
     * Return the current writer. Writing to this is equivalent to calling
     * {@link #print(String)} and friends. That is, persistent writers will
     * still be called. 
     * 
     * <p>Note that this is not necessarily the same writer as was last passed 
     * to {@link #pushWriter(Writer)}.
     * 
     * @return The current writer
     */
    public Writer getWriter()
    {
        // Wrap in tee to ensure that persistent writers are still called when
        // this writer is used.
    	if(teeWriter == null) {
    		teeWriter = new TeeWriter(internalWriter, persistentWriters);
    	}
        return teeWriter;
    }
    
    /**
     * Push a new writer to print to. The current writer is flushed and then
     * pushed onto a stack. It can be restored with {@link #popWriter()}
     * 
     * @param writer The new writer to write to. If null, the a NullWriter is
     *      used and all output will be dropped.
     */
    public void pushWriter(Writer writer)
    {
        printWriter.flush();
        persistentPrintWriter.flush();
        
        stack.push(new StackEntry(internalWriter, printWriter));
        
        this.internalWriter = writer != null ? writer : new NullWriter();
        this.printWriter = asPrintWriter(internalWriter);
        teeWriter = null;
    }
    
    private PrintWriter asPrintWriter(Writer writer)
    {
        return writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer, true);
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
        printWriter.flush();
        persistentPrintWriter.flush();
        
        final Writer oldInternal = this.internalWriter;
        
        StackEntry e = stack.pop();
        this.internalWriter = e.internal;
        this.printWriter = e.wrapped;
        
        teeWriter = null;
        
        return oldInternal;
    }
    
    /**
     * Add a writer that will receive <em>all</em> print output regardless of
     * push/pop.
     * 
     * @param writer the writer to add
     */
    public void addPersistentWriter(Writer writer)
    {
        this.persistentWriters.addWriter(writer);
        teeWriter = null;
    }
    
    /**
     * Remove a writer previously added with {@link #addPersistentWriter(Writer)}
     * 
     * @param writer the writer to remove
     */
    public void removePersistentWriter(Writer writer)
    {
        this.persistentWriters.removeWriter(writer);
        teeWriter = null;
    }
    
    public Printer print(String output)
    {
        this.printWriter.print(output);
        this.persistentPrintWriter.print(output);
        return this;
    }
    
    public Printer print(String format, Object ... args)
    {
    	try {
    		this.printWriter.printf(format, args);
    	}
    	catch (Exception e) {
    		System.out.println("!!");	
    	}
    	this.persistentPrintWriter.printf(format, args);
        return this;
    }
    
    public Formatter asFormatter()
    {
        return new Formatter(this.getWriter());
    }
    
    public Printer startNewLine()
    {
        if(!atStartOfLine)
        {
            this.printWriter.append('\n');
            this.persistentPrintWriter.append('\n');
        }
        return this;
    }
    
    public Printer flush()
    {
        this.printWriter.flush();
        this.persistentPrintWriter.flush();
        return this;
    }
    
    public Printer warn(String message)
    {
        if(printWarnings)
        {
            print(message);
        }
        warnings.add(message);
        return this;
    }

    public Printer warn(String format, Object ... args)
    {
        if(printWarnings)
        {
            print(format, args);
        }

        warnings.add(String.format(format, args));
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
     * @param message message
     * @return this
     */
    public Printer error(String message)
    {
        print("\nError: " + message);
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
            printWriter.write(SPACES, 0, c);
            persistentPrintWriter.write(SPACES, 0, c);
            n -= c;
        }
        return this;
    }

    public List<String> getWarningsAndClear() {
        List<String> copy = new ArrayList<>(warnings);
        warnings.clear();
        return copy;
    }
    
    private static class StackEntry
    {
        final Writer internal;
        final PrintWriter wrapped;
        
        public StackEntry(Writer internal, PrintWriter wrapped)
        {
            this.internal = internal;
            this.wrapped = wrapped;
        }
    }
}
