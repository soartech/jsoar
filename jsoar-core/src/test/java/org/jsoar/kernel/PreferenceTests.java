/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import org.junit.Test;

/** @author ray */
public class PreferenceTests extends FunctionalTestHarness {
  @Test(timeout = 10000)
  public void testPreferenceSemantics() throws Exception {
    runTest("testPreferenceSemantics", -1);
  }

  @Test
  public void testTieImpasse() throws Exception {
    runTest("testTieImpasse", 1);
  }

  @Test
  public void testConflictImpasse() throws Exception {
    runTest("testConflictImpasse", 1);
  }

  @Test
  public void testConstraintFailureImpasse() throws Exception {
    runTest("testConstraintFailureImpasse", 1);
  }

  @Test
  public void testOperatorNoChangeImpasse() throws Exception {
    runTest("testOperatorNoChangeImpasse", 2);
  }

  @Test
  public void testStateNoChangeImpasse() throws Exception {
    runTest("testStateNoChangeImpasse", 1);
  }

  @Test
  // this test actually touches code outside of the decision procedure, in RecognitionMemory, but
  // it's still preference related
  public void testORejectsFirst() throws Exception {
    runTest("testORejectsFirst", 1);
  }

  @Test
  public void testDeallocation() throws Exception {
    runTest("testDeallocation", 6);
  }
}
