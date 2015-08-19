/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 7, 2009
 */
package org.jsoar.util;


import android.test.AndroidTestCase;

import java.net.URL;

/**
 * @author ray
 */
public class FileToolsTest extends AndroidTestCase
{

    public void testAsUrl() throws Exception
    {
        URL url = FileTools.asUrl("http://www.google.com");
        assertEquals("http://www.google.com", url.toExternalForm());
        assertNull(FileTools.asUrl("/not/a/url"));
    }
    
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
    
    public void testCanGetSimpleExtension()
    {
        assertEquals("txt", FileTools.getExtension("foo.txt"));
    }
    
    public void testReturnsEmptyStringWhenFileNameEndsWithADot()
    {
        assertEquals("", FileTools.getExtension("foo."));
    }
    
    public void testCanGetExtensionForFullPath()
    {
        assertEquals("png", FileTools.getExtension("/a/path.with.dots/foo.png"));
    }
    
    public void testReturnsNullWhenThereIsNoExtension()
    {
        assertNull(FileTools.getExtension("foo"));
    }
    
    public void testCanTellThatExtensionIsMissingWhenAPathContainsDots()
    {
        assertNull(FileTools.getExtension("/a/path.with.dots/foo"));
    }
}
