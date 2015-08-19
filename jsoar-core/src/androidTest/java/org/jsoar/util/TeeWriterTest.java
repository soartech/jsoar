/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 20, 2008
 */
package org.jsoar.util;

import android.test.AndroidTestCase;

import java.io.StringWriter;

/**
 * @author ray
 */
public class TeeWriterTest extends AndroidTestCase
{

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void setUp() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void tearDown() throws Exception
    {
    }

    /**
     * Test method for {@link org.jsoar.util.TeeWriter#write(char[], int, int)}.
     */
    public void testWrite() throws Exception
    {
        StringWriter first = new StringWriter();
        StringWriter second = new StringWriter();
        StringWriter third = new StringWriter();
        
        TeeWriter tee = null;
        try 
        {
            tee = new TeeWriter(first, second, third);
            
            final String text = "This is some text";
            tee.append(text);
            tee.flush();
            assertEquals(text, first.getBuffer().toString());
            assertEquals(text, second.getBuffer().toString());
            assertEquals(text, third.getBuffer().toString());
        }
        finally 
        {
            tee.close();
        }
        
    }

}
