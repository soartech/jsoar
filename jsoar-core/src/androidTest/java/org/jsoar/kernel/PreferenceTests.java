/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


/**
 * @author ray
 */
public class PreferenceTests extends FunctionalTestHarness
{
    public void testPreferenceSemantics() throws Exception
    {
        runTest("testPreferenceSemantics", -1);
    }
    
    public void testTieImpasse() throws Exception
    {
        runTest("testTieImpasse", 1);
    }
    
    public void testConflictImpasse() throws Exception
    {
        runTest("testConflictImpasse", 1);
    }
    
    public void testConstraintFailureImpasse() throws Exception
    {
        runTest("testConstraintFailureImpasse", 1);
    }
    
    public void testOperatorNoChangeImpasse() throws Exception
    {
        runTest("testOperatorNoChangeImpasse", 2);
    }
    
    public void testStateNoChangeImpasse() throws Exception
    {
        runTest("testStateNoChangeImpasse", 1);
    }
            
    // this test actually touches code outside of the decision procedure, in RecognitionMemory, but it's still preference related
    public void testORejectsFirst() throws Exception
    {
        runTest("testORejectsFirst", 1);
    }

    public void testDeallocation() throws Exception
    {
        runTest("testDeallocation", 6);
    }

}
