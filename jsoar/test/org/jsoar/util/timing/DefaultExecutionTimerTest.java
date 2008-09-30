/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author ray
 */
public class DefaultExecutionTimerTest
{
    /**
     * Test method for {@link org.jsoar.util.timing.DefaultExecutionTimer#DefaultExecutionTimer()}.
     */
    @Test
    public void testNewInstanceDefaultServiceLoader()
    {
        ExecutionTimer timer = DefaultExecutionTimer.newInstance();
        assertNotNull(timer);
        assertTrue(((DefaultExecutionTimer) timer).__testGetSource() instanceof WallclockExecutionTimeSource);
    }
}
