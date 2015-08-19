/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

import android.test.AndroidTestCase;

/**
 * @author ray
 */
public class DefaultExecutionTimerTest extends AndroidTestCase
{
    /**
     * Test method for {@link org.jsoar.util.timing.DefaultExecutionTimer#DefaultExecutionTimer()}.
     */
    public void testNewInstanceDefaultServiceLoader()
    {
        ExecutionTimer timer = DefaultExecutionTimer.newInstance();
        assertNotNull(timer);
        assertTrue(((DefaultExecutionTimer) timer).__testGetSource() instanceof WallclockExecutionTimeSource);
    }
}
