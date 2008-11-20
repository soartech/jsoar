/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.events.InputCycleEvent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.Arguments;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;

/**
 * Adds an augmentation to the input-link with the current cycle count. The
 * default name of the attribute is "cycle-count".
 * 
 * @author ray
 */
public class CycleCountInput
{
    private final InputOutput io;
    private final SoarEventManager events;
    private final Listener listener;
    private int count = 1;
    private Wme wme;
    
    /**
     * Construct a new cycle count input object.
     * 
     * @param io The I/O interface
     * @param events The event manager. If not <code>null</code> then this object
     *     will automatically register for input events and update the input-link.
     *     Otherwise, is it up to calling code to call {@link #update()} manually
     *     during the input cycle.
     */
    public CycleCountInput(InputOutput io, SoarEventManager events)
    {
        Arguments.checkNotNull(io, "io");
        
        this.io = io;
        this.events = events;
        if(this.events != null)
        {
            this.listener = new Listener();
            this.events.addListener(InputCycleEvent.class, listener);
        }
        else
        {
            this.listener = null;
        }
    }
    
    /**
     * Updates the input-link. This should only be called during the input cycle
     * and then only if this object was constructed without a reference to the
     * event manager.
     */
    public void update()
    {
        // TODO on init-soar, the count should go back to 0. It would be better to the
        // the cycle count from the agent (or I/O?) rather than maintaining it
        // manually like this.
        
        final SymbolFactory syms = io.getSymbols();
        final IntegerSymbol newCount = syms.createInteger(count++);
        if(wme == null)
        {
            wme = io.addInputWme(io.getInputLink(), syms.createString("cycle-count"), newCount);
        }
        else
        {
            wme = io.updateInputWme(wme, newCount);
        }
    }
    
    /**
     * Dispose this object, removing the cycle count from the input link and 
     * unregistering from the event manager if necessary 
     */
    public void dispose()
    {
        if(this.events != null)
        {
            this.events.removeListener(InputCycleEvent.class, listener);
        }
        
        // TODO remove cycle count WME
    }
    
    private class Listener implements SoarEventListener
    {
        /* (non-Javadoc)
         * @see org.jsoar.kernel.events.SoarEventListener#onEvent(org.jsoar.kernel.events.SoarEvent)
         */
        @Override
        public void onEvent(SoarEvent event)
        {
            update();
        }
    }
}
