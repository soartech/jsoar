/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.io.InputOutput;


/**
 * Event fired during the input cycle. This event is the best/safest time to
 * add input WMEs to an agent.
 * 
 * @author ray
 */
public class InputCycleEvent extends AbstractInputOutputEvent
{
    /**
     * Construct a new event
     * 
     * @param io the I/O interface
     */
    public InputCycleEvent(InputOutput io)
    {
        super(io);
    }
    
}
