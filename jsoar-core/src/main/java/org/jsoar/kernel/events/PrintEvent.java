/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.tracing.PrintEventWriter;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.events.SoarEvent;

/**
 * Event fired when the agent prints to its printer. This event is only
 * fired when the printer is flushed.
 * 
 * @author ray
 * @see Printer
 * @see PrintEventWriter
 */
public class PrintEvent implements SoarEvent
{
    private final String text;
    
    /**
     * @param text the text
     */
    public PrintEvent(String text)
    {
        this.text = text;
    }

    /**
     * @return the text of the event
     */
    public String getText()
    {
        return text;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getText();
    }
}