/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2009
 */
package org.jsoar.util.events;

/**
 * Utility methods for use with SoarEvent classes.
 * 
 * @see SoarEvent
 * @see SoarEventManager
 * @see SoarEventListener
 * @author ray
 */
public class SoarEvents
{
    /**
     * Adds an event listener that listens for a single event and then removes 
     * itself from the event manager. Note that the event may be called from 
     * another thread, possibly before this method even returns.
     *  
     * @param <T> The event type to register for.
     * @param manager the event manager
     * @param klass the event type
     * @param listener the listener
     */
    public static <T extends SoarEvent> void listenForSingleEvent(final SoarEventManager manager, final Class<T> klass, final SoarEventListener listener)
    {
        manager.addListener(klass, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                try
                {
                    listener.onEvent(event);
                }
                finally
                {
                    manager.removeListener(klass, this);
                }
            }});
    }
}
