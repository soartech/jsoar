/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.util.events.SoarEvent;

/**
 * Base class for an I/O event, i.e. an event that has a pointer to
 * an {@link InputOutput} object.
 * 
 * @author ray
 */
public abstract class AbstractInputOutputEvent implements SoarEvent
{
    private final InputOutput io;

    /**
     * Construct a new event
     * 
     * @param io The I/O interface
     */
    public AbstractInputOutputEvent(InputOutput io)
    {
        this.io = io;
    }
    
    /**
     * @return The InputOutput object that is the source of this event
     */
    public InputOutput getInputOutput()
    {
        return io;
    }
}
