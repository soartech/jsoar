/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 19, 2010
 */
package org.jsoar.util.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    @BeforeEach
    void setUp() throws Exception
    {
        this.agent = new Agent();
        this.interp = new DefaultInterpreter(agent);
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
    @Test
    void testPassesCommandContextToCommand() throws Exception
    {
        final AtomicReference<SoarCommandContext> context = new AtomicReference<>();
        interp.addCommand("testCommandContext", new SoarCommand()
        {
            @Override
            public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
            {
                context.set(commandContext);
                return null;
            }
            
            @Override
            public Object getCommand()
            {
                // todo - when implementing picocli, return the runnable
                return null;
            }
        });
        
        final URL result = getClass().getResource("DefaultInterpreterTest_testPassesCommandContextToCommand.soar");
        assertNotNull(result, "Couldn't find test resource");
        
        interp.source(result);
        
        assertNotNull(context.get());
        assertEquals(result.toExternalForm(), context.get().getSourceLocation().getFile());
        assertEquals(3, context.get().getSourceLocation().getLine() + 1);
    }
    
    @Test
    void testCanChooseACommandBasedOnAPrefix() throws Exception
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
            public Object getCommand()
            {
                // todo - when implementing picocli, return the runnable
                return null;
            }
        });
        interp.eval("testCa");
        assertTrue(called.get(), "Expected testCanChoose command to be called");
    }
    
    @Test()
    public void testThrowsAnExceptionWhenACommandPrefixIsAmbiguous()
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
            public Object getCommand()
            {
                // todo - when implementing picocli, return the runnable
                return null;
            }
        };
        interp.addCommand("testCanChoose", command);
        interp.addCommand("testCanAlsoChoose", command);
        assertThrows(SoarException.class, () -> interp.eval("testCan"));
    }
    
    @Test
    void testCanLoadRelativePathInJar() throws Exception
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
        SoarCommands.source(agent.getInterpreter(), url);
        
        assertEquals(1, agent.getProductions().getProductionCount(), "Expected a rule to be loaded");
    }
}
