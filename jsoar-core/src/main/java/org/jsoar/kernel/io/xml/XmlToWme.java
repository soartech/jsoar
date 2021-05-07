/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 3, 2009
 */
package org.jsoar.kernel.io.xml;

import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.symbols.Identifier;
import org.w3c.dom.Element;

/**
 * Interface for an object that knows how to convert an XML DOM tree into Soar working memory
 * elements. The details of the XML format and the conversion process are specific to the
 * implementation.
 *
 * @author ray
 * @see DefaultXmlToWme
 * @see SoarTechXmlToWme
 * @see AutoTypeXmlToWme
 * @see ManualTypeXmlToWme
 */
public interface XmlToWme {
  /**
   * Convert the XML tree rooted at the given element to working memory, returning the root
   * identifier of the new WM tree. It is the caller's responsibility to ensure that this method is
   * being called at the appropriate time, i.e. during an {@link InputEvent}, or from a {@link
   * RhsFunctionHandler}.
   *
   * @param element the root of the DOM tree
   * @return root identifier of new structure
   */
  Identifier fromXml(Element element);
}
