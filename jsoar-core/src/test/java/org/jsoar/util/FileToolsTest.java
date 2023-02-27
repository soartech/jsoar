/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 7, 2009
 */
package org.jsoar.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;

import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class FileToolsTest
{
    
    @Test
    void testAsUrl() throws Exception
    {
        URL url = FileTools.asUrl("http://www.google.com");
        assertEquals("http://www.google.com", url.toExternalForm());
        assertNull(FileTools.asUrl("/not/a/url"));
    }
    
    @Test
    void testReplaceIllegalChars()
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
    
    @Test
    void testCanGetSimpleExtension()
    {
        assertEquals("txt", FileTools.getExtension("foo.txt"));
    }
    
    @Test
    void testReturnsEmptyStringWhenFileNameEndsWithADot()
    {
        assertEquals("", FileTools.getExtension("foo."));
    }
    
    @Test
    void testCanGetExtensionForFullPath()
    {
        assertEquals("png", FileTools.getExtension("/a/path.with.dots/foo.png"));
    }
    
    @Test
    void testReturnsNullWhenThereIsNoExtension()
    {
        assertNull(FileTools.getExtension("foo"));
    }
    
    @Test
    void testCanTellThatExtensionIsMissingWhenAPathContainsDots()
    {
        assertNull(FileTools.getExtension("/a/path.with.dots/foo"));
    }
}
