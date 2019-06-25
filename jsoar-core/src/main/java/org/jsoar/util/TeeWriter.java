/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 20, 2008
 */
package org.jsoar.util;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A writer that simultaneously writes to one or more writers.
 * 
 * @author ray
 */
public class TeeWriter extends Writer
{
    private final List<Writer> writers = new CopyOnWriteArrayList<Writer>();
    
    /**
     * Construct a new TeeWriter
     * 
     * @param writers List of writers to write to
     */
    public TeeWriter(Writer ... writers)
    {
        for(Writer writer : writers)
        {
            this.writers.add(writer);
        }
    }
    
    public void addWriter(Writer writer)
    {
        this.writers.add(writer);
    }
    
    public void removeWriter(Writer writer)
    {
        this.writers.remove(writer);
    }
    
    /* (non-Javadoc)
     * @see java.io.Writer#close()
     */
    @Override
    public void close() throws IOException
    {
        for(Writer w : writers)
        {
            w.close();
        }
    }

    /* (non-Javadoc)
     * @see java.io.Writer#flush()
     */
    @Override
    public void flush() throws IOException
    {
        for(Writer w : writers)
        {
            w.flush();
        }
    }

    /* (non-Javadoc)
     * @see java.io.Writer#write(char[], int, int)
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
        for(Writer w : writers)
        {
            w.write(cbuf, off, len);
        }
    }

}
