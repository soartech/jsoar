/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.kernel.tracing;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.events.PrintEvent;
import org.jsoar.util.NullWriter;
import org.jsoar.util.events.SoarEventManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class PrintEventWriterTest
{
    private SoarEventManager events;
    private Printer printer;

    @Before
    public void setUp() throws Exception
    {
        this.events = new SoarEventManager();
        this.printer = new Printer(new NullWriter());
        this.printer.addPersistentWriter(new PrintEventWriter(events));
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testPrintEventIsFiredOnFlush()
    {
        final AtomicReference<String> text = new AtomicReference<String>();
        events.addListener(PrintEvent.class, event -> text.set(((PrintEvent) event).getText()));
        
        printer.print("abcdefghijk");
        assertNull(text.get());
        printer.flush();
        assertEquals("abcdefghijk", text.get());
    }
    
    @Test
    public void testDoesNotFirePrintEventIfNothingHasBeenPrinted()
    {
        final AtomicReference<String> text = new AtomicReference<String>();
        events.addListener(PrintEvent.class, event -> text.set(((PrintEvent) event).getText()));
        
        assertNull(text.get());
        printer.flush();
        assertNull(text.get());
    }
}
