/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 10, 2010
 */
package org.jsoar.kernel.commands;


import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.commands.DefaultSoarCommandContext;

import java.io.StringWriter;

/**
 * @author ray
 */
public class WatchCommandTest extends AndroidTestCase
{
    private Trace trace;
    private WatchCommand watch;

    @Override
    public void setUp() throws Exception
    {
        this.trace = new Trace(new Printer(new StringWriter()));
        this.watch = new WatchCommand(trace);
    }

    public void testBacktracing() throws SoarException
    {
        verifyOption(Category.BACKTRACING, "backtracing");
    }

    public void testChunks() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_CHUNKS, "chunks");
    }
    public void testDefault() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_DEFAULT_PRODS, "default", "D");
    }
    public void testJustifications() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_JUSTIFICATIONS, "justifications");
    }
    public void testTemplates() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_TEMPLATES, "template", "T");
    }
    public void testUserProds() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_USER_PRODS, "user");
    }
    public void testPreferences() throws SoarException
    {
        verifyOption(Category.FIRINGS_PREFERENCES, "preferences", "r");
    }
    public void testGds() throws SoarException
    {
        verifyOption(Category.GDS, "gds");
    }
    public void testIndifferentSelection() throws SoarException
    {
        verifyOption(Category.INDIFFERENT, "indifferent-selection");
    }
    public void testPhases() throws SoarException
    {
        verifyOption(Category.PHASES, "phases");
    }
    public void testRl() throws SoarException
    {
        verifyOption(Category.RL, "rl", "R");
    }
    public void testVerbose() throws SoarException
    {
        verifyOption(Category.VERBOSE, "verbose");
    }
    public void testWorkingMemory() throws SoarException
    {
        verifyOption(Category.WM_CHANGES, "wmes");
    }
    public void testWaterfall() throws SoarException
    {
        verifyOption(Category.WATERFALL, "waterfall", "W");
    }
    public void testProductions() throws SoarException
    {
        verifyOption(Category.FIRINGS_OF_USER_PRODS, "productions", "P");
        verifyOption(Category.FIRINGS_OF_JUSTIFICATIONS, "productions", "P");
        verifyOption(Category.FIRINGS_OF_CHUNKS, "productions", "P");
    }
    
    public void testNone() throws SoarException
    {
        verifyWatchLevel(0, "watch", "-N");
        verifyWatchLevel(0, "watch", "--none");
    }
    
    public void testExplicitLevelShort() throws SoarException
    {
        for(int i = 0; i <= 5; i++)
        {
            verifyWatchLevel(i, "watch", "-l", Integer.toString(i));
        }
    }
    public void testExplicitLevelLong() throws SoarException
    {
        for(int i = 0; i <= 5; i++)
        {
            verifyWatchLevel(i, "watch", "--level", Integer.toString(i));
        }
    }
    
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
        watch.execute(DefaultSoarCommandContext.empty(), args);
        for(Category c : Category.values())
        {
            if(c.isWatchable() && c.isActiveInWatchLevel(level))
            {
                Assert.assertTrue("Category " + c + " should be on for watch level " + level,
                        trace.isEnabled(c));
            }
        }
    }
    
    private void verifyOption(Category c, String longOpt) throws SoarException
    {
        verifyOption(c, longOpt, Character.toString(longOpt.charAt(0)));
    }
    
    private void verifyOption(Category c, String longOpt, String shortOpt) throws SoarException
    {
        Assert.assertFalse(trace.isEnabled(c));
        watch.execute(DefaultSoarCommandContext.empty(), new String[] { "watch", "-" + shortOpt });
        Assert.assertTrue(trace.isEnabled(c));
        watch.execute(DefaultSoarCommandContext.empty(), new String[] { "watch", "-" + shortOpt, "remove" });
        Assert.assertFalse(trace.isEnabled(c));
        
        // Now with the long version
        watch.execute(DefaultSoarCommandContext.empty(), new String[] { "watch", "--" + longOpt });
        Assert.assertTrue(trace.isEnabled(c));
        watch.execute(DefaultSoarCommandContext.empty(), new String[] { "watch", "--" + longOpt, "0" });
        Assert.assertFalse(trace.isEnabled(c));
    }
}
