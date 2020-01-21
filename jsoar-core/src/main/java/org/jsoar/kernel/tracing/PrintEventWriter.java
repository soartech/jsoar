/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.kernel.tracing;

import java.io.IOException;
import java.io.Writer;

import org.jsoar.kernel.events.PrintEvent;
import org.jsoar.util.events.SoarEventManager;


/**
 * @author ray
 * @see Printer
 * @see PrintEvent
 */
public class PrintEventWriter extends Writer
{
    private final SoarEventManager events;
    private final StringBuilder buffer = new StringBuilder();
    
    /**
     * Construct a new print event writer with the given event manager
     * 
     * @param events the event manager to fire print events on
     */
    public PrintEventWriter(SoarEventManager events)
    {
        this.events = events;
    }

    /* (non-Javadoc)
     * @see java.io.Writer#close()
     */
    @Override
    public void close() throws IOException
    {
        flush();
    }

    /* (non-Javadoc)
     * @see java.io.Writer#flush()
     */
    @Override
    public void flush() throws IOException
    {
        final String text = buffer.toString();
        if(text.length() == 0)
        {
            return;
        }
        
        final PrintEvent event = new PrintEvent(text);
        buffer.setLength(0);
        events.fireEvent(event);
    }

    /* (non-Javadoc)
     * @see java.io.Writer#write(char[], int, int)
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
        buffer.append(cbuf, off, len);
    }

}
