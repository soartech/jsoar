/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 7, 2009
 */
package org.jsoar.util;


import static org.junit.Assert.*;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class FileToolsTest
{

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testAsUrl() throws Exception
    {
        URL url = FileTools.asUrl("http://www.google.com");
        assertEquals("http://www.google.com", url.toExternalForm());
        assertNull(FileTools.asUrl("/not/a/url"));
    }
    
    @Test
    public void testReplaceIllegalChars()
    {
        assertEquals("eye _ ball", FileTools.replaceIllegalCharacters("eye ? ball", "_"));
        assertEquals("__", FileTools.replaceIllegalCharacters("*?", "_"));
        assertEquals("__", FileTools.replaceIllegalCharacters("<>", "_"));
        assertEquals("__", FileTools.replaceIllegalCharacters("][", "_"));
        assertEquals("__", FileTools.replaceIllegalCharacters("\\/", "_"));
        assertEquals("__", FileTools.replaceIllegalCharacters(":;", "_"));
        assertEquals("__", FileTools.replaceIllegalCharacters("\",", "_"));
        assertEquals("__", FileTools.replaceIllegalCharacters("+=", "_"));
        assertEquals("__", FileTools.replaceIllegalCharacters("|,", "_"));
        assertEquals("abcdefg.txt", FileTools.replaceIllegalCharacters("abcdefg.txt", "_"));
    }
}
