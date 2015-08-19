/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2009
 */
package org.jsoar.util.events;


import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ray
 */
public class SoarEventsTest extends AndroidTestCase
{
    private static class TestEvent implements SoarEvent
    {
    };
    
    public void testListenForSingleEvent()
    {
        final List<TestEvent> caughtEvents = new ArrayList<TestEvent>();
        final SoarEventManager manager = new SoarEventManager();
        SoarEvents.listenForSingleEvent(manager, TestEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                caughtEvents.add((TestEvent) event);
            }}); 
        
        assertEquals(0, caughtEvents.size());
        
        final TestEvent event = new TestEvent();
        manager.fireEvent(event);
        assertEquals(1, caughtEvents.size());
        assertSame(event, caughtEvents.get(0));
        
        manager.fireEvent(event);
        assertEquals(1, caughtEvents.size());
    }
}
