/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2009
 */
package org.jsoar.util.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class SoarEventsTest
{
    private static class TestEvent implements SoarEvent
    {
    }
    
    @Test
    public void testListenForSingleEvent()
    {
        final List<TestEvent> caughtEvents = new ArrayList<>();
        final SoarEventManager manager = new SoarEventManager();
        SoarEvents.listenForSingleEvent(manager, TestEvent.class, event -> caughtEvents.add((TestEvent) event));
        
        assertEquals(0, caughtEvents.size());
        
        final TestEvent event = new TestEvent();
        manager.fireEvent(event);
        assertEquals(1, caughtEvents.size());
        assertSame(event, caughtEvents.get(0));
        
        manager.fireEvent(event);
        assertEquals(1, caughtEvents.size());
    }
}
