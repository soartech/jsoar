/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 21, 2010
 */
package org.jsoar.script;


import static org.junit.Assert.*;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.AdaptableContainer;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScriptCommandTest
{
    private Adaptable context;
    private ScriptCommand command;
    
    @Before
    public void setUp() throws Exception
    {
        context = AdaptableContainer.from(new RhsFunctionManager(null));
        command = new ScriptCommand(context);
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test(expected=SoarException.class)
    public void testThrowsAnExceptionForUnknownScriptEngines() throws Exception
    {
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "script", "unknown-script-engine" });
    }
    
    @Test
    public void testCanEvalScriptCode() throws Exception
    {
        final String result = command.execute(DefaultSoarCommandContext.empty(), new String[] { "script", "javascript", "'hi there'" });
        assertEquals("hi there", result);
    }
    
    @Test
    public void testInstallsRhsFunctionHandler() throws Exception
    {
        // Initialize javascript engine
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "script", "javascript" });
        
        final RhsFunctionManager rhsFuncs = Adaptables.adapt(context, RhsFunctionManager.class);
        assertNotNull(rhsFuncs);
        
        final RhsFunctionHandler handler = rhsFuncs.getHandler("javascript");
        assertNotNull(handler);
    }
}
