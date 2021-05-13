/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** @author ray */
public class DefaultExecutionTimerTest {

  @Test
  public void testNewInstanceDefaultServiceLoader() {
    ExecutionTimer timer = DefaultExecutionTimer.newInstance();
    assertNotNull(timer);
    assertTrue(
        ((DefaultExecutionTimer) timer).testGetSource() instanceof WallclockExecutionTimeSource);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNewInstanceThrowsExceptionIfSourceIsNull() {
    DefaultExecutionTimer.newInstance(null);
  }
}
