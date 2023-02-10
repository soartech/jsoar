/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 10, 2010
 */
package org.jsoar.kernel.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class TraceCommandTest
{
    private Trace trace;
    private TraceCommand traceCommand;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        Agent agent = new Agent();
        this.trace = agent.getTrace();
        this.traceCommand = new TraceCommand(agent);
    }
    
    @Test
    public void testBacktracing() throws SoarException
    {
        verifyOption(Category.BACKTRACING, "backtracing");
    }
    
    @Test
    public void testChunks() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_CHUNKS, "chunks");
    }
    
    @Test
    public void testDefault() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_DEFAULT_PRODS, "default", "D");
    }
    
    @Test
    public void testJustifications() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_JUSTIFICATIONS, "justifications");
    }
    
    @Test
    public void testTemplates() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_TEMPLATES, "template", "T");
    }
    
    @Test
    public void testUserProds() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_USER_PRODS, "user");
    }
    
    @Test
    public void testPreferences() throws SoarException
    {
        verifyOption(Category.FIRINGS_PREFERENCES, "preferences", "r");
    }
    
    @Test
    public void testGds() throws SoarException
    {
        verifyOption(Category.GDS, "gds");
    }
    
    @Test
    public void testIndifferentSelection() throws SoarException
    {
        verifyOption(Category.INDIFFERENT, "indifferent-selection");
    }
    
    @Test
    public void testPhases() throws SoarException
    {
        verifyOption(Category.PHASES, "phases");
    }
    
    @Test
    public void testRl() throws SoarException
    {
        verifyOption(Category.RL, "rl", "R");
    }
    
    @Test
    public void testVerbose() throws SoarException
    {
        verifyOption(Category.VERBOSE, "assertions", "A");
    }
    
    @Test
    public void testWorkingMemory() throws SoarException
    {
        verifyOption(Category.WM_CHANGES, "wmes");
    }
    
    @Test
    public void testWaterfall() throws SoarException
    {
        verifyOption(Category.WATERFALL, "waterfall", "W");
    }
    
    @Test
    public void testProductions() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_USER_PRODS, "productions", "P");
        verifyOption(Category.FIRINGS_OF_JUSTIFICATIONS, "productions", "P");
        verifyOption(Category.FIRINGS_OF_CHUNKS, "productions", "P");
    }
    
    @Test
    public void testNone() throws SoarException
    {
        verifyWatchLevel(0, "watch", "-N");
        verifyWatchLevel(0, "watch", "--none");
    }
    
    @Test
    public void testExplicitLevelShort() throws SoarException
    {
        for(int i = 0; i <= 5; i++)
        {
            verifyWatchLevel(i, "watch", "-l", Integer.toString(i));
        }
    }
    
    @Test
    public void testExplicitLevelLong() throws SoarException
    {
        for(int i = 0; i <= 5; i++)
        {
            verifyWatchLevel(i, "watch", "--level", Integer.toString(i));
        }
    }
    
    @Test
    public void testImplicitWatchLevel() throws SoarException
    {
        for(int i = 0; i <= 5; i++)
        {
            verifyWatchLevel(i, "watch", Integer.toString(i));
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    private void verifyWatchLevel(int level, String... args) throws SoarException
    {
        trace.disableAll();
        traceCommand.execute(DefaultSoarCommandContext.empty(), args);
        for(Category c : Category.values())
        {
            if(c.isWatchable() && c.isActiveInWatchLevel(level))
            {
                assertTrue(trace.isEnabled(c), "Category " + c + " should be on for watch level " + level);
            }
        }
    }
    
    private void verifyOption(Category c, String longOpt) throws SoarException
    {
        verifyOption(c, longOpt, Character.toString(longOpt.charAt(0)));
    }
    
    private void verifyOption(Category c, String longOpt, String shortOpt) throws SoarException
    {
        assertFalse(trace.isEnabled(c));
        traceCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "trace", "-" + shortOpt });
        assertTrue(trace.isEnabled(c));
        traceCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "trace", "-" + shortOpt, "remove" });
        assertFalse(trace.isEnabled(c));
        
        // Now with the long version
        traceCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "trace", "--" + longOpt });
        assertTrue(trace.isEnabled(c));
        traceCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "trace", "--" + longOpt, "0" });
        assertFalse(trace.isEnabled(c));
    }
}
