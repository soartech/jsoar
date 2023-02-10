/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 7, 2010
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StandardFunctionsTest
{
    private Agent agent;
    private StringWriter outputWriter;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        this.agent = new Agent(getClass().getSimpleName());
        this.agent.getPrinter().addPersistentWriter(outputWriter = new StringWriter());
        this.agent.getTrace().disableAll();
    }
    
    @AfterEach
    public void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
    @Test
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
        assertTrue(output.matches("Succeeded: testSucceededRhsFunction: something, nothing\n.*"), "Unexpected output: " + output);
    }
    
    @Test
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
        assertTrue(output.matches("Succeeded: testFailedRhsFunction: nothing, something\n.*"), "Unexpected output: " + output);
        
    }
    
}
