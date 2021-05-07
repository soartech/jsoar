/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * The <code>XMLPath</code> represents a location in the XML tree. The location is changed by
 * pushing and popping tags relative to the currently location. A method is provided to output the
 * current location in the form of tag names separated by dots.
 *
 * @author chris.kawatsu
 */
class XmlPath {
  private final LinkedList<String> path;

  /** Create an empty <code>XMLPath</code>. */
  public XmlPath() {
    path = new LinkedList<String>();
  }

  /**
   * Add a tag to the current path.
   *
   * @param name - the name of the XML tag
   */
  public void pushTag(String name) {
    path.push(name);
  }

  /** Remove the top level tag from the current path. */
  public void popTag() {
    path.pop();
  }

  /**
   * Get the current location in the XML tag tree, with tags separated by dots.
   *
   * @return A <code>String</code> representing the current location in the XML tree.
   */
  @Override
  public String toString() {
    final StringBuilder fullPath = new StringBuilder();
    boolean first = true;
    final Iterator<String> it = path.descendingIterator();
    while (it.hasNext()) {
      final String s = it.next();
      if (first) {
        fullPath.append(s);
        first = false;
        continue;
      }
      fullPath.append(".");
      fullPath.append(s);
    }

    return fullPath.toString();
  }
}
