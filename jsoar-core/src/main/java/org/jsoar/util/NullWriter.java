/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.util;

import java.io.IOException;
import java.io.Writer;

/**
 * A writer that does nothing.
 * 
 * @author ray
 */
public class NullWriter extends Writer
{
    /**
     * 
     */
    public NullWriter()
    {
    }
    
    /**
     * @param lock
     */
    public NullWriter(Object lock)
    {
        super(lock);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.io.Writer#close()
     */
    @Override
    public void close() throws IOException
    {
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.io.Writer#flush()
     */
    @Override
    public void flush() throws IOException
    {
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.io.Writer#write(char[], int, int)
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
    }
    
}
