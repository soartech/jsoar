/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 19, 2010
 */
package org.jsoar.util.commands;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class DefaultInterpreterTest
{
    private Agent agent;
    private DefaultInterpreter interp;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.interp = new DefaultInterpreter(agent);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
    @Test
    public void testPassesCommandContextToCommand() throws Exception
    {
        final AtomicReference<SoarCommandContext> context = new AtomicReference<SoarCommandContext>();
        interp.addCommand("testCommandContext", new SoarCommand()
        {
            @Override
            public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
            {
                context.set(commandContext);
                return null;
            }
            @Override
            public Object getCommand() {
                //todo - when implementing picocli, return the runnable
                return null;
            }
        });
        
        final URL result = getClass().getResource("DefaultInterpreterTest_testPassesCommandContextToCommand.soar");
        assertNotNull("Couldn't find test resource", result);
        
        interp.source(result);
        
        assertNotNull(context.get());
        assertEquals(result.toExternalForm(), context.get().getSourceLocation().getFile());
        assertEquals(3, context.get().getSourceLocation().getLine() + 1);
    }

    @Test
    public void testCanChooseACommandBasedOnAPrefix() throws Exception
    {
        final AtomicBoolean called = new AtomicBoolean(false);
        interp.addCommand("testCanChoose", new SoarCommand()
        {
            @Override
            public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
            {
                called.set(true);
                return null;
            }
            @Override
            public Object getCommand() {
                //todo - when implementing picocli, return the runnable
                return null;
            }
        });
        interp.eval("testCa");
        assertTrue("Expected testCanChoose command to be called", called.get());
    }
    
    @Test(expected=SoarException.class)
    public void testThrowsAnExceptionWhenACommandPrefixIsAmbiguous() throws Exception
    {
        final AtomicBoolean called = new AtomicBoolean(false);
        final SoarCommand command = new SoarCommand()
        {
            @Override
            public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
            {
                called.set(true);
                return null;
            }
            @Override
            public Object getCommand() {
                //todo - when implementing picocli, return the runnable
                return null;
            }
        };
        interp.addCommand("testCanChoose", command);
        interp.addCommand("testCanAlsoChoose", command);
        interp.eval("testCan");
        assertFalse("Expected an ambiguous command exception", called.get());
    }
    
    @Test
    public void testCanLoadRelativePathInJar() throws Exception
    {
        // it turns out that loading a file from a jar whose path contains a "." causes problems
        // this problem can arise via a sequence like this (which NGS used to do):
        //
        // pushd .
        // source myfile.soar
        //
        // jsoar was modified (both default and Tcl interps) to normalize these paths
        // this test ensures it stays fixed: it uses a jar with a file that does the above
        // specifically, test.soar contains:
        //
        // pushd .
        // source test2.soar
        //
        // test2.soar contains a rule. So we simply test that the rule gets sourced.
        // When it failed before the fix, it did so by throwing an exception.
        
        // trying to construct "jar:file:test.jar!/test.soar" with the proper path to the jar
        // probably there is a better way, but this is what worked first
        
        String path = getClass().getResource("test.jar").toExternalForm();
        String inputFile = "jar:" + path + "!/test.soar";
        
        URL url = new URL(inputFile);
        SoarCommands.source(agent.getInterpreter(), url );
        
        assertTrue("Expected a rule to be loaded", agent.getProductions().getProductionCount() == 1);
    }
}
