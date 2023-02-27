/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author ray
 */
class PreferenceTests extends FunctionalTestHarness
{
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testPreferenceSemantics() throws Exception
    {
        runTest("testPreferenceSemantics", -1);
    }
    
    @Test
    void testTieImpasse() throws Exception
    {
        runTest("testTieImpasse", 1);
    }
    
    @Test
    void testConflictImpasse() throws Exception
    {
        runTest("testConflictImpasse", 1);
    }
    
    @Test
    void testConstraintFailureImpasse() throws Exception
    {
        runTest("testConstraintFailureImpasse", 1);
    }
    
    @Test
    void testOperatorNoChangeImpasse() throws Exception
    {
        runTest("testOperatorNoChangeImpasse", 2);
    }
    
    @Test
    void testStateNoChangeImpasse() throws Exception
    {
        runTest("testStateNoChangeImpasse", 1);
    }
    
    @Test
    // this test actually touches code outside of the decision procedure, in RecognitionMemory, but it's still preference related
    public void testORejectsFirst() throws Exception
    {
        runTest("testORejectsFirst", 1);
    }
    
    @Test
    void testDeallocation() throws Exception
    {
        runTest("testDeallocation", 6);
    }
    
}
