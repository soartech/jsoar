/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 20, 2008
 */
package org.jsoar.kernel.tracing;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * @author ray
 */
public class PrinterTest extends AndroidTestCase
{

    /**
     * Test method for {@link org.jsoar.kernel.tracing.Printer#pushWriter(java.io.Writer, boolean)}.
     */
    public void testPushWriter()
    {
        StringWriter first = new StringWriter();
        StringWriter second = new StringWriter();
        
        Printer printer = new Printer(first);
        printer.print("first");
        assertEquals("first", first.toString());
        assertEquals("", second.toString());
        
        printer.pushWriter(second);
        printer.print("second");
        assertEquals("first", first.toString());
        assertEquals("second", second.toString());
        
        Writer popped = printer.popWriter();
        assertSame(second, popped);
        printer.print("first");
        assertEquals("firstfirst", first.toString());
        assertEquals("second", second.toString());
    }

    public void testPopWriterThrowsNoSuchElementException()
    {
        Printer printer = new Printer(new StringWriter());
        try {
            printer.popWriter();
            Assert.fail("Should have thrown");
        }catch(NoSuchElementException e){
            //No message.  Just need to get here
        }
    }
    
    /**
     * Test method for {@link org.jsoar.kernel.tracing.Printer#spaces(int)}.
     */
    public void testSpaces()
    {
        StringWriter first = new StringWriter();
        
        Printer printer = new Printer(first);
        printer.spaces(500);
        String result = first.toString();
        char[] spaceArray = new char[500];
        Arrays.fill(spaceArray, ' ');
        assertEquals(new String(spaceArray), result);
    }

}
