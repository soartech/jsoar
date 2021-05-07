/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 7, 2009
 */
package org.jsoar.util;

import static org.junit.Assert.*;

import java.net.URL;
import org.junit.Test;

/** @author ray */
public class FileToolsTest {

  @Test
  public void testAsUrl() throws Exception {
    URL url = FileTools.asUrl("http://www.google.com");
    assertEquals("http://www.google.com", url.toExternalForm());
    assertNull(FileTools.asUrl("/not/a/url"));
  }

  @Test
  public void testReplaceIllegalChars() {
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
  public void testCanGetSimpleExtension() {
    assertEquals("txt", FileTools.getExtension("foo.txt"));
  }

  @Test
  public void testReturnsEmptyStringWhenFileNameEndsWithADot() {
    assertEquals("", FileTools.getExtension("foo."));
  }

  @Test
  public void testCanGetExtensionForFullPath() {
    assertEquals("png", FileTools.getExtension("/a/path.with.dots/foo.png"));
  }

  @Test
  public void testReturnsNullWhenThereIsNoExtension() {
    assertNull(FileTools.getExtension("foo"));
  }

  @Test
  public void testCanTellThatExtensionIsMissingWhenAPathContainsDots() {
    assertNull(FileTools.getExtension("/a/path.with.dots/foo"));
  }
}
