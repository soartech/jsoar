/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 19, 2010
 */
package org.jsoar.util.commands;


import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ray
 */
public class DefaultInterpreterTest extends AndroidTestCase
{
    private Agent agent;
    private DefaultInterpreter interp;
    
    /**
     * @throws java.lang.Exception
     */
    @Override
    public void setUp() throws Exception
    {
        this.agent = new Agent(getContext());
        this.interp = new DefaultInterpreter(agent, getContext().getAssets());
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
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
        });
        
        final URL result = getClass().getResource("DefaultInterpreterTest_testPassesCommandContextToCommand.soar");
        assertNotNull("Couldn't find test resource", result);
        
        interp.source(result);
        
        assertNotNull(context.get());
        assertEquals(result.toExternalForm(), context.get().getSourceLocation().getFile());
        assertEquals(3, context.get().getSourceLocation().getLine() + 1);
    }

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
        });
        interp.eval("testCa");
        assertTrue("Expected testCanChoose command to be called", called.get());
    }
    
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
        };
        interp.addCommand("testCanChoose", command);
        interp.addCommand("testCanAlsoChoose", command);
        try {
            interp.eval("testCan");
            assertFalse("Expected an ambiguous command exception", called.get());
        }catch (SoarException e){
            Assert.assertEquals("Ambiguous command 'testCan'. Could be one of 'testCanAlsoChoose, testCanChoose", e.getMessage());
        }
    }

    @Ignore("We shouldn't be loading anything out of jars on Android")
    public void IGNOREDtestCanLoadRelativePathInJar() throws Exception
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
