/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.kernel.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.events.PrintEvent;
import org.jsoar.util.NullWriter;
import org.jsoar.util.events.SoarEventManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class PrintEventWriterTest
{
    private SoarEventManager events;
    private Printer printer;
    
    @BeforeEach
    void setUp() throws Exception
    {
        this.events = new SoarEventManager();
        this.printer = new Printer(new NullWriter());
        this.printer.addPersistentWriter(new PrintEventWriter(events));
    }
    
    @AfterEach
    void tearDown() throws Exception
    {
    }
    
    @Test
    void testPrintEventIsFiredOnFlush()
    {
        final AtomicReference<String> text = new AtomicReference<>();
        events.addListener(PrintEvent.class, event -> text.set(((PrintEvent) event).getText()));
        
        printer.print("abcdefghijk");
        assertNull(text.get());
        printer.flush();
        assertEquals("abcdefghijk", text.get());
    }
    
    @Test
    void testDoesNotFirePrintEventIfNothingHasBeenPrinted()
    {
        final AtomicReference<String> text = new AtomicReference<>();
        events.addListener(PrintEvent.class, event -> text.set(((PrintEvent) event).getText()));
        
        assertNull(text.get());
        printer.flush();
        assertNull(text.get());
    }
}
