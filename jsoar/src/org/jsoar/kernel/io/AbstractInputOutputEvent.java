/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.events.SoarEvent;

/**
 * @author ray
 */
public abstract class AbstractInputOutputEvent implements SoarEvent
{
    private final InputOutput io;

    /**
     * @param io
     */
    public AbstractInputOutputEvent(InputOutput io)
    {
        this.io = io;
    }
    
    public InputOutput getInputOutput()
    {
        return io;
    }
}
