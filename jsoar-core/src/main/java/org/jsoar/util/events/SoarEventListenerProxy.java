/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 25, 2010
 */
package org.jsoar.util.events;

/**
 * A wrapper around an event listener. This is stupid...
 * 
 * <p> ... but necessary due to an issue with the JavaScript scripting engine.
 * Namely, if you register a JavaScript function directly as a handler,
 * using the auto-generated proxy, an UndeclaredThrowableException is thrown
 * when the listener is removed because of some issue with the proxy's
 * equals() method. Fun!!
 * 
 * @author ray
 */
public class SoarEventListenerProxy implements SoarEventListener
{
    private final SoarEventListener target;
    
    public SoarEventListenerProxy(SoarEventListener target)
    {
        this.target = target;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.events.SoarEventListener#onEvent(org.jsoar.util.events.SoarEvent)
     */
    @Override
    public void onEvent(SoarEvent event)
    {
        target.onEvent(event);
    }
}
