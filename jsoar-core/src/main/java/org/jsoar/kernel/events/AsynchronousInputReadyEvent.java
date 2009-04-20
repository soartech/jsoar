/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.runtime.WaitRhsFunction;


/**
 * This event is fired by input components to notify run control components,
 * such as {@link WaitRhsFunction}, that new input is available for the agent.
 * If the agent happens to be suspended, it can be restarted.
 * 
 * <p>Any input component that provides input based on an external, asynchronous
 * trigger (for example an input component that receives network messages and
 * provides them to the agent) should fire this event.
 * 
 * @author ray
 */
public class AsynchronousInputReadyEvent extends AbstractInputOutputEvent
{
    /**
     * Construct a new event
     * 
     * @param io the I/O interface
     */
    public AsynchronousInputReadyEvent(InputOutput io)
    {
        super(io);
    }
    
}
