/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.io.InputOutput;


/**
 * Event fired when the top-state is removed
 * 
 * @author ray
 */
public class TopStateRemovedEvent extends AbstractInputOutputEvent
{
    /**
     * Construct a new event
     * 
     * @param io The I/O interface
     */
    public TopStateRemovedEvent(InputOutput io)
    {
        super(io);
    }
}
