/*
 * Copyright (c) 2013 Soar Technology Inc.
 *
 * Created on January 07, 2013
 */package org.jsoar.tcl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.net.URL;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the "cmd" RHS function.
 * 
 * @author charles.newton
 */
public class TclCmdTest
{
    private Agent agent;
    private StringWriter outputWriter;
    private SoarTclInterface ifc;

    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        ifc = SoarTclInterface.findOrCreate(agent);
        this.agent.getPrinter().addPersistentWriter(
                outputWriter = new StringWriter());
        this.agent.getTrace().disableAll();
        this.agent.initialize();
    }

    @After
    public void tearDown() throws Exception
    {
        if (this.agent != null)
        {
            SoarTclInterface.dispose(ifc);
            ifc = null;
            this.agent.dispose();
            this.agent = null;
        }
    }
    
    @Test
    public void testCmdPrintD1S1() throws Exception
    {
        agent.getProductions().loadProduction("testCmdPrintD1S1 (state <s> ^superstate nil) --> (write (cmd print -d 1 <s>))");
        outputWriter.flush();
        agent.runFor(1, RunType.DECISIONS);
        String output = outputWriter.getBuffer().toString();
        assertEquals("(S1 ^io I1 ^reward-link R1 ^smem S2 ^superstate nil ^type state)\n", output);
    }
    
    @Test
    public void testCmdFC() throws Exception
    {
        agent.getProductions().loadProduction("testCmdPrintFC (state <s> ^superstate nil) --> (write (cmd firing-counts))");
        outputWriter.flush();
        agent.runFor(1, RunType.DECISIONS);
        String output = outputWriter.getBuffer().toString();
        assertEquals("    1:  testCmdPrintFC", output);
    }
    
    @Test
    public void testRHSFunction() throws Exception
    {
        // cmd only takes in Soar commands as arguments, not RHS functions.
        agent.getProductions().loadProduction("testCmdRHSFunction (state <s> ^superstate nil) --> (write (cmd make-constant-symbol |test|))");
        outputWriter.flush();
        agent.runFor(1, RunType.DECISIONS);
        String output = outputWriter.getBuffer().toString();
        assertTrue(output.startsWith("\nError:"));
    }
    
    @Test
    public void testNewline() throws Exception
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
    
    @Test
    public void testTclGetCommand() throws Exception
    {
        ifc.eval("sp {wait (state <s> ^superstate nil) --> (wait)}");
        SourceLocation location = agent.getProductions().getProduction("wait").getLocation();
        SoarCommand pwd = ifc.getCommand("pwd", location);
        final SoarCommandContext commandContext = new DefaultSoarCommandContext(location);
        String[] args = new String[1];
        String ret = pwd.execute(commandContext, args);
        assertNotNull(ret);
        assertTrue(ret.length() > 0);
    }
    
    @Test
    public void testTclSource() throws Exception
    {
        // Make sure we source with the TCL interpreter (not the default interpreter).
        URL sourceTestLoc = getClass().getResource("/" + SoarTclInterfaceTest.class.getCanonicalName().replace('.', '/') + "_sourceResource.soar");
        System.out.println(sourceTestLoc.toExternalForm());
        ifc.eval("sp {testSource (state <s> ^superstate nil) --> (write (crlf) (cmd source |" + sourceTestLoc.toExternalForm() + "|))}");
        outputWriter.flush();
        agent.runFor(1, RunType.DECISIONS);
        assertNotNull(ifc.getAgent().getProductions().getProduction("top-state*propose*wait"));
    }
}