/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 16, 2008
 */
package org.jsoar.kernel;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.jsoar.kernel.Trace.Category;
import org.junit.Before;
import org.junit.Test;


/**
 * @author ray
 */
public class TraceTest
{
    private StringWriter output;
    private Printer printer;
    private Trace trace;
    
    @Before
    public void setUp()
    {
        output = new StringWriter();
        printer = new Printer(output);
        trace = new Trace(printer);
    }

    @Test
    public void testEnabledByDefault()
    {
        assertTrue(trace.isEnabled());
    }
    
    @Test
    public void testGlobalEnable()
    {
        trace.setEnabled(false);
        trace.setEnabled(Category.TRACE_BACKTRACING_SYSPARAM, true);
        trace.print(Category.TRACE_BACKTRACING_SYSPARAM, "hello");
        output.flush();
        assertEquals("", output.toString());
        
        trace.setEnabled(true);
        trace.print(Category.TRACE_BACKTRACING_SYSPARAM, "hello");
        output.flush();
        assertEquals("hello", output.toString());
    }
    
    @Test
    public void testPerCategoryEnable()
    {
        trace.setEnabled(Category.TRACE_BACKTRACING_SYSPARAM, true);
        trace.print(Category.TRACE_BACKTRACING_SYSPARAM, "hello");
        output.flush();
        assertEquals("hello", output.toString());
        
        trace.setEnabled(Category.TRACE_BACKTRACING_SYSPARAM, false);
        trace.print(Category.TRACE_BACKTRACING_SYSPARAM, "more");
        output.flush();
        assertEquals("hello", output.toString());
        
        trace.setEnabled(Category.TRACE_BACKTRACING_SYSPARAM, true);
        trace.print(Category.TRACE_BACKTRACING_SYSPARAM, "-even-more");
        output.flush();
        assertEquals("hello-even-more", output.toString());
    }
}
