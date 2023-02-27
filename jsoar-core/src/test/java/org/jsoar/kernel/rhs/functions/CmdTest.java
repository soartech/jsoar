/*
 * Copyright (c) 2013 Soar Technology Inc.
 *
 * Created on January 07, 2013
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CmdTest
{
    private Agent agent;
    private StringWriter outputWriter;
    
    @BeforeEach
    void setUp() throws Exception
    {
        this.agent = new Agent(false);
        this.agent.getPrinter().pushWriter(outputWriter = new StringWriter());
        this.agent.getTrace().disableAll();
        
        // Since this compares text and .initialize() writes a \n to the trace
        // These tests will fail unless the trace is off when .initialize() is
        // called.
        this.agent.initialize();
    }
    
    @AfterEach
    void tearDown() throws Exception
    {
        if(this.agent != null)
        {
            this.agent.dispose();
            this.agent = null;
        }
    }
    
    @Test
    void testCmdPrintD1S1() throws Exception
    {
        agent.getProductions().loadProduction("testCmdPrintD1S1 (state <s> ^superstate nil) --> (write (cmd print -d 1 <s>))");
        outputWriter.flush();
        agent.runFor(1, RunType.DECISIONS);
        String output = outputWriter.getBuffer().toString();
        assertEquals("(S1 ^epmem E1 ^io I1 ^reward-link R1 ^smem S2 ^superstate nil ^type state)\n", output);
    }
    
    @Test
    void testCmdFC() throws Exception
    {
        agent.getProductions().loadProduction("testCmdPrintFC (state <s> ^superstate nil) --> (write (cmd fc))");
        outputWriter.flush();
        agent.runFor(1, RunType.DECISIONS);
        String output = outputWriter.getBuffer().toString();
        assertEquals("    1:  testCmdPrintFC", output);
    }
    
    @Test
    void testRHSFunction() throws Exception
    {
        // cmd only takes in Soar commands as arguments, not RHS functions.
        agent.getProductions().loadProduction("testCmdRHSFunction (state <s> ^superstate nil) --> (write (cmd make-constant-symbol |test|))");
        outputWriter.flush();
        agent.runFor(1, RunType.DECISIONS);
        String output = outputWriter.getBuffer().toString();
        assertTrue(output.startsWith("\nError:"));
    }
    
    @Test
    void testNewline() throws Exception
    {
        // This shouldn't run both "print -d 1 S1" and "fc", i.e. \n should be
        // passed as an argument, not parsed as a command delimiter.
        agent.getProductions().loadProduction("testNewline (state <s> ^superstate nil) --> (write (cmd print -d 1 <s> |\n| fc))");
        outputWriter.flush();
        agent.runFor(1, RunType.DECISIONS);
        String output = outputWriter.getBuffer().toString();
        assertFalse(output.startsWith("(S1"));
        assertFalse(output.startsWith("(S1 ^io I1 ^reward-link R1 ^smem S2 ^superstate nil ^type state)"));
        assertTrue(output.startsWith("No production named"));
    }
}
