/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.debugger;

import static org.junit.Assert.*;

import org.junit.Test;

/** @author ray */
public class DefaultDebuggerProviderTest {

  @Test
  public void testThatThisclassIsInTheRightPackageToAvoidBreakingReflection() {
    assertEquals(
        "The debugger provider's name or package has chanaged, but is referenced reflectively.",
        org.jsoar.kernel.DefaultDebuggerProvider.DEFAULT_CLASS,
        DefaultDebuggerProvider.class.getCanonicalName());
  }
}
