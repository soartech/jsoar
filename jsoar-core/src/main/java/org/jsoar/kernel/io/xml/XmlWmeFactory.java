/*
 * (c) 2009  Soar Technology, Inc.
 *
 * Created on Apr 27, 2009
 */
package org.jsoar.kernel.io.xml;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
public interface XmlWmeFactory
{
    void addWme(Identifier id, Symbol attr, Symbol value);
}
