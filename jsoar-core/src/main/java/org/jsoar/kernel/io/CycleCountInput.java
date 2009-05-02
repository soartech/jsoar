/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.events.BeforeInitSoarEvent;
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
    private static final int START = 1; // TODO: I think this should be 0. Why isn't it?
    private final InputOutput io;
    private final SoarEventManager events;
    private final InputListener listener;
    private final InitSoarListener initListener;
    
    private int count = START;
    private Wme wme;
    
    /**
     * Construct a new cycle count input object.
     * 
     * @param io The I/O interface
     * @param events The event manager. This object
     *     will automatically register for input events and update the input-link.
     */
    public CycleCountInput(InputOutput io, SoarEventManager events)
    {
        Arguments.checkNotNull(io, "io");
        Arguments.checkNotNull(events, "events");
        
        this.io = io;
        this.events = events;
        this.listener = new InputListener();
        this.initListener = new InitSoarListener();
        this.events.addListener(InputCycleEvent.class, listener);
        this.events.addListener(BeforeInitSoarEvent.class, initListener);
    }
    
    /**
     * Dispose this object, removing the cycle count from the input link and 
     * unregistering from the event manager if necessary 
     */
    public void dispose()
    {
        this.events.removeListener(null, listener);
        this.events.removeListener(null, initListener);
        
        // Schedule removal of wme at next input cycle.
        // TODO: I think this should be handled by InputOutput
        if(wme != null)
        {
            this.events.addListener(InputCycleEvent.class, new CleanupListener(this.events, wme));
            this.wme = null;
        }
    }
    
    /**
     * Updates the input-link. This should only be called during the input cycle.
     */
    private void update()
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
    
    private class InputListener implements SoarEventListener
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
    
    private class InitSoarListener implements SoarEventListener
    {
        /* (non-Javadoc)
         * @see org.jsoar.util.events.SoarEventListener#onEvent(org.jsoar.util.events.SoarEvent)
         */
        @Override
        public void onEvent(SoarEvent event)
        {
            wme = null;
            count = START;
        }
        
    }
    
    private static class CleanupListener implements SoarEventListener
    {
        private final SoarEventManager manager;
        private final Wme wme;

        /**
         * @param wme
         */
        public CleanupListener(SoarEventManager manager, Wme wme)
        {
            this.manager = manager;
            this.wme = wme;
        }

        /* (non-Javadoc)
         * @see org.jsoar.util.events.SoarEventListener#onEvent(org.jsoar.util.events.SoarEvent)
         */
        @Override
        public void onEvent(SoarEvent event)
        {
            InputCycleEvent ie = (InputCycleEvent) event;
            
            ie.getInputOutput().removeInputWme(wme);
            
            this.manager.removeListener(null, this);
            
        }
        
    }
}
