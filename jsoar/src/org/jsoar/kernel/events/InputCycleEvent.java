/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.io.InputOutput;


/**
 * @author ray
 */
public class InputCycleEvent extends AbstractInputOutputEvent
{

    /**
     * @param io
     */
    public InputCycleEvent(InputOutput io)
    {
        super(io);
    }
    
}
