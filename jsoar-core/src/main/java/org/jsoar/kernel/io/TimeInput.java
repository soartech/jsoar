/*
 * Copyright (c) 2009  Dave Ray < daveray@gmail.com>
 *
 * Created on June 11, 2009
 */
package org.jsoar.kernel.io;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.util.Arguments;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;

/**
 * Adds an augmentation to the input-link with the current world time,
 * including year, month, day, hour, minute, second, and seconds-from-start.
 * The default name of the attribute is "time".
 * 
 * @author quist
 */
public class TimeInput
{
    private final InputOutput io;
    private final SoarEventManager events;
    private final InputListener listener;
    private final InitSoarListener initListener;
    
    private Calendar startTime;
    private InputWme wme;
    private final Map<String, InputWme> childWmes = new HashMap<String, InputWme>();
    
    /**
     * Construct a new world-time input object.
     * 
     * @param io The I/O interface
     * @param events The event manager. This object
     *     will automatically register for input events and update the input-link.
     */
    public TimeInput(InputOutput io, SoarEventManager events)
    {
        Arguments.checkNotNull(io, "io");
        Arguments.checkNotNull(events, "events");
        
        this.io = io;
        this.events = events;
        this.listener = new InputListener();
        this.initListener = new InitSoarListener();
        this.events.addListener(InputEvent.class, listener);
        this.events.addListener(BeforeInitSoarEvent.class, initListener);
    }
    
    /**
     * Dispose this object, removing its WMEs from the input link and 
     * unregistering from the event manager if necessary 
     */
    public void dispose()
    {
        this.events.removeListener(null, listener);
        this.events.removeListener(null, initListener);
        
        // Schedule removal of wme at next input cycle.
        if(wme != null)
        {
            this.wme.remove();
            this.wme = null;
            for (InputWme childWme : childWmes.values())
            {
                childWme.remove();
            }
            childWmes.clear();
        }
    }
    
    private void setChildWme(String name, int value)
    {
        if (!childWmes.containsKey(name))
        {
            InputWme childWme = InputWmes.add(wme, name, value);
            childWmes.put(name, childWme);
        }
        else
        {
            InputWmes.update(childWmes.get(name), value);
        }
    }
    
    /**
     * Updates the input-link. This should only be called during the input cycle.
     */
    private void update()
    {
        if (startTime == null)
        {
            startTime = Calendar.getInstance();
        }
        
        if (wme == null)
        {
            wme = InputBuilder.create(io).push("time").getWme(null);
        }
        
        Calendar currentTime = Calendar.getInstance();
        setChildWme("second", currentTime.get(Calendar.SECOND));
        setChildWme("minute", currentTime.get(Calendar.MINUTE));
        setChildWme("hour", currentTime.get(Calendar.HOUR));
        setChildWme("day", currentTime.get(Calendar.DAY_OF_MONTH));
        setChildWme("month", currentTime.get(Calendar.MONTH));
        setChildWme("year", currentTime.get(Calendar.YEAR));
        
        long msecFromStart = currentTime.getTimeInMillis() - startTime.getTimeInMillis();
        setChildWme("seconds-from-start", (int)(msecFromStart / 1000.0));
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
            childWmes.clear();
            startTime = Calendar.getInstance();
        }
    }
}