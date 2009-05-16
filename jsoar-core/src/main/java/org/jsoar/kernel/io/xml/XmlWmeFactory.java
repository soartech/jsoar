/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 27, 2009
 */
package org.jsoar.kernel.io.xml;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Interface use by objects that convert XML to working memory. Intended
 * to abstract the difference between constructing a new WME in a RHS
 * function versus on the input-link.
 * 
 * <p>See {@link InputXmlWmeFactory} and {@link RhsFunctionXmlWmeFactory}
 * for basic implementations of this interface
 * 
 * @author ray
 */
public interface XmlWmeFactory
{
    /**
     * Add a new WME. Note that this method does not return a new Wme. This is
     * because Wmes created in a RHS function are not actually "created" until
     * later so there is no WME to return.
     * 
     * @param id the id of the new WME
     * @param attr the attribute of the new WME
     * @param value the value of the new WME
     */
    void addWme(Identifier id, Symbol attr, Symbol value);
}
