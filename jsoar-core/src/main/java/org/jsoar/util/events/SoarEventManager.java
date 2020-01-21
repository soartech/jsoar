/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 26, 2008
 */
package org.jsoar.util.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author ray
 */
public class SoarEventManager
{
    /**
     * List of listeners for events of any type
     */
    private List<SoarEventListener> listenersForAny = new CopyOnWriteArrayList<SoarEventListener>();
    
    /**
     * Listeners indexed by event type
     */
    private Map<Class<? extends SoarEvent>, List<SoarEventListener>> listeners = Collections.synchronizedMap(new HashMap<Class<? extends SoarEvent>, List<SoarEventListener>>());

    public SoarEventManager()
    {
    }
    
    /**
     * Add a listener for a particular type of event. A listener may be registered 
     * for more than one type of event.
     * 
     * <p>It is safe to call this method from any thread, or from an event listener callback.
     * 
     * @param <T> The type of event
     * @param klass The class of event. If null of SoarEvent, then the listener will receive all
     *          events.
     * @param listener The listener
     * @throws NullPointerException if listener is <code>null</code>
     */
    public <T extends SoarEvent> void addListener(Class<T> klass, SoarEventListener listener)
    {
        if(listener == null)
        {
            throw new NullPointerException("listener");
        }
        getListenersForEventType(klass).add(listener);
    }
    
    /**
     * Remove a listener previously added with {@link #addListener(Class, SoarEventListener)}.
     * 
     * <p>It is safe to call this method from any thread, or from an event listener callback.
     * 
     * @param <T> The event type
     * @param klass The class of the event type to remove the listener from, or <code>null</code>
     *      to completely remove the listener from the manager.
     * @param listener The listener to remove
     */
    public <T extends SoarEvent> void removeListener(Class<T> klass, SoarEventListener listener)
    {
        if(klass == null)
        {
            listenersForAny.remove(listener);
            synchronized(listeners)
            {
                for(List<SoarEventListener> list : listeners.values())
                {
                    list.remove(listener);
                }
            }
        }
        else
        {
            getListenersForEventType(klass).remove(listener);
        }
    }
    
    /**
     * Fire the given event to all listeners.
     * 
     * <p>It is safe to call this method from any thread, but may negatively affect
     * listeners that expect to be called on a particular thread.
     * 
     * <p>It is safe to call this method from an event listener callback.
     * 
     * @param <T> The event type
     * @param event The event object. Must be non-null.
     * @throws NullPointerException if event is <code>null</code>.
     */
    public <T extends SoarEvent> void fireEvent(T event)
    {
        fireEvent(event, event.getClass());
    }
    
    /**
     * Fire the given event to all listeners registered for the given event 
     * type. This extended version of {@link #fireEvent(SoarEvent)} allows a
     * particular event class to be specified so that sub-classes of an existing
     * event class can be properly routed to listeners of that parent class.
     * 
     * @param <T> Event type
     * @param event The event object. Must be non-null.
     * @param eventType Event type used to route to listeners
     */
    public <T extends SoarEvent> void fireEvent(T event, Class<? extends T> eventType)
    {
        if(event == null)
        {
            throw new NullPointerException("event");
        }
        
        for(SoarEventListener l : getListenersForEventType(eventType))
        {
            l.onEvent(event);
        }
        for(SoarEventListener l : listenersForAny)
        {
            l.onEvent(event);
        }
    }
    
    private <T extends SoarEvent> List<SoarEventListener> getListenersForEventType(Class<T> klass)
    {
        if(klass == null || klass.equals(SoarEvent.class))
        {
            return listenersForAny;
        }
        synchronized(listeners)
        {
            List<SoarEventListener> list = listeners.get(klass);
            if(list == null)
            {
                list = new CopyOnWriteArrayList<SoarEventListener>();
                listeners.put(klass, list);
            }
            return list;
        }
    }
}
