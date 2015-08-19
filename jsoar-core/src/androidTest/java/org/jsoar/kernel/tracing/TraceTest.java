/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 16, 2008
 */
package org.jsoar.kernel.tracing;

import android.test.AndroidTestCase;

import org.jsoar.kernel.tracing.Trace.Category;

import java.io.StringWriter;


/**
 * @author ray
 */
public class TraceTest extends AndroidTestCase
{
    private StringWriter output;
    private Printer printer;
    private Trace trace;
    
    @Override
    public void setUp()
    {
        output = new StringWriter();
        printer = new Printer(output);
        trace = new Trace(printer);
    }

    public void testEnabledByDefault()
    {
        assertTrue(trace.isEnabled());
    }
    
    public void testGlobalEnable()
    {
        trace.setEnabled(Category.BACKTRACING, true);
        trace.setEnabled(false);
        trace.print(Category.BACKTRACING, "hello");
        output.flush();
        assertEquals("", output.toString());
        
        trace.setEnabled(true);
        trace.print(Category.BACKTRACING, "hello");
        output.flush();
        assertEquals("hello", output.toString());
    }
    
    public void testPerCategoryEnable()
    {
        trace.setEnabled(Category.BACKTRACING, true);
        trace.print(Category.BACKTRACING, "hello");
        output.flush();
        assertEquals("hello", output.toString());
        
        trace.setEnabled(Category.BACKTRACING, false);
        trace.print(Category.BACKTRACING, "more");
        output.flush();
        assertEquals("hello", output.toString());
        
        trace.setEnabled(Category.BACKTRACING, true);
        trace.print(Category.BACKTRACING, "-even-more");
        output.flush();
        assertEquals("hello-even-more", output.toString());
    }
}
