/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2009
 */
package org.jsoar.kernel.io.xml;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Implementation of {@link XmlWmeFactory} that creates {@link InputWme}s.
 * 
 * @author ray
 */
public class InputXmlWmeFactory implements XmlWmeFactory
{
    private final InputOutput io;
    
    /**
     * Constrcut a WME factory that uses the given {@link InputOutput} object
     * to construct WMEs.
     * 
     * @param io the input/output object to use
     */
    public InputXmlWmeFactory(InputOutput io)
    {
        this.io = io;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.xml.XmlWmeFactory#addWme(org.jsoar.kernel.symbols.Identifier, org.jsoar.kernel.symbols.Symbol, org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public void addWme(Identifier id, Symbol attr, Symbol value)
    {
        io.addInputWme(id, attr, value);
    }

}
