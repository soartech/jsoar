/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 7, 2010
 */
package org.jsoar.kernel.rhs.functions;


import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;

import java.io.StringWriter;

public class StandardFunctionsTest extends AndroidTestCase
{
    private Agent agent;
    private StringWriter outputWriter;
    
    @Override
    public void setUp() throws Exception
    {
        this.agent = new Agent(getClass().getSimpleName(), getContext());
        this.agent.getPrinter().addPersistentWriter(outputWriter = new StringWriter());
        this.agent.getTrace().disableAll();
    }

    @Override
    public void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
    public void testSucceededRhsFunctionPrintsAMessageAndHaltsTheAgent() throws Exception
    {
        this.agent.getProductions().loadProduction(
                "testSucceededRhsFunction " +
                "(state <s> ^superstate nil)" +
                "-->" +
                "(succeeded something nothing)");
        this.agent.runForever();
        assertTrue(this.agent.getReasonForStop().contains("halted"));
        final String output = outputWriter.toString();
        
        // Note that this tests the exact print output of the function
        assertTrue("Unexpected output: " + output, 
                   output.matches("Succeeded: testSucceededRhsFunction: something, nothing\n.*"));
    }
    
    public void testFailedRhsFunctionPrintsAMessageAndHaltsTheAgent() throws Exception
    {
        this.agent.getProductions().loadProduction(
                "testFailedRhsFunction " +
                "(state <s> ^superstate nil)" +
                "-->" +
                "(succeeded nothing something)");
        this.agent.runForever();
        assertTrue(this.agent.getReasonForStop().contains("halted"));
        final String output = outputWriter.toString();
        
        // Note that this tests the exact print output of the function
        assertTrue("Unexpected output: " + output, 
                   output.matches("Succeeded: testFailedRhsFunction: nothing, something\n.*"));
        
    }

}
