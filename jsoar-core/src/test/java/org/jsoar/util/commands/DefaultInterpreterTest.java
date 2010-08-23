/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 19, 2010
 */
package org.jsoar.util.commands;


import static org.junit.Assert.*;

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
        });
        
        final URL result = getClass().getResource("DefaultInterpreterTest_testPassesCommandContextToCommand.soar");
        assertNotNull("Couldn't find test resource", result);
        
        interp.source(result);
        
        assertNotNull(context.get());
        assertEquals(result.toExternalForm(), context.get().getSourceLocation().getFile());
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
        };
        interp.addCommand("testCanChoose", command);
        interp.addCommand("testCanAlsoChoose", command);
        interp.eval("testCan");
        assertFalse("Expected an ambiguous command exception", called.get());
    }
}
