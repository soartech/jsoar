/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 4, 2010
 */
package org.jsoar.kernel.commands;


import android.test.AndroidTestCase;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.events.SoarEventManager;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SourceCommandTest extends AndroidTestCase
{
    private static class TestAdapter implements SourceCommandAdapter
    {
        final List<File> files = new ArrayList<File>();
        final List<String> codes = new ArrayList<String>();
        final List<URL> urls = new ArrayList<URL>();
        
        @Override
        public void eval(File file) throws SoarException
        {
            this.files.add(file);
        }

        @Override
        public String eval(String code) throws SoarException
        {
            this.codes.add(code);
            return "";
        }

        @Override
        public void eval(URL url) throws SoarException
        {
            this.urls.add(url);
        }
    }

    @Override
    public void setUp() throws Exception
    {
    }

    @Override
    public void tearDown() throws Exception
    {
    }
    
    public void testShortReloadThrowsExceptionIfNoPreviousCommand()
    {
        final TestAdapter a = new TestAdapter();
        final SourceCommand command = new SourceCommand(a, new SoarEventManager(), getContext().getAssets());
        try {
            command.execute(DefaultSoarCommandContext.empty(), new String[] {"source", "-r"});
            fail("Should have thrown exception");
        } catch (SoarException e) {
            assertEquals("No previous file to reload", e.getMessage());
        }
    }
    
    public void testLongReloadThrowsExceptionIfNoPreviousCommand()
    {
        final TestAdapter a = new TestAdapter();
        final SourceCommand command = new SourceCommand(a, new SoarEventManager(), getContext().getAssets());
        try {
            command.execute(DefaultSoarCommandContext.empty(), new String[] {"source", "--reload"});
            fail("Should have thrown exception");
        } catch (SoarException e) {
            assertEquals("No previous file to reload", e.getMessage());
        }
    }
    
    public void testReloadCallsLastSourcedFile() throws Exception
    {
        final TestAdapter a = new TestAdapter();
        String[] commandStrings = new String[] {"source", "-a", "test.soar"};
        final SourceCommand command = new SourceCommand(a, new SoarEventManager(), getContext().getAssets());
        command.execute(DefaultSoarCommandContext.empty(), commandStrings);
        //In Android, these are sourcing assets, not files, so there are not files in the list
        assertEquals(1, a.codes.size());
        assertEquals("", a.codes.get(0));
        assertTrue(Arrays.deepEquals(command.getLastTopLevelCommand(), commandStrings));

        a.codes.clear();
        command.execute(DefaultSoarCommandContext.empty(), commandStrings);
        assertEquals(1, a.codes.size());
        assertEquals("", a.codes.get(0));
        assertTrue(Arrays.deepEquals(command.getLastTopLevelCommand(), commandStrings));

        a.codes.clear();
        command.execute(DefaultSoarCommandContext.empty(), commandStrings);
        assertEquals(1, a.codes.size());
        assertEquals("", a.codes.get(0));
        assertTrue(Arrays.deepEquals(command.getLastTopLevelCommand(), commandStrings));
    }

    public void testReloadCallsLastSourcedUrl() throws Exception
    {
        final TestAdapter a = new TestAdapter();
        final SourceCommand command = new SourceCommand(a, new SoarEventManager(), getContext().getAssets());
        command.execute(DefaultSoarCommandContext.empty(), new String[] {"source", "-v", "http://daveray.com/test.soar"});
        assertEquals(1, a.urls.size());
        assertEquals("http://daveray.com/test.soar", a.urls.get(0).toExternalForm());
        
        a.urls.clear();
        command.execute(DefaultSoarCommandContext.empty(), new String[] {"source", "-r"});
        assertEquals(1, a.urls.size());
        assertEquals("http://daveray.com/test.soar", a.urls.get(0).toExternalForm());
        
        a.urls.clear();
        command.execute(DefaultSoarCommandContext.empty(), new String[] {"source", "--reload"});
        assertEquals(1, a.urls.size());
        assertEquals("http://daveray.com/test.soar", a.urls.get(0).toExternalForm());
    }
}
