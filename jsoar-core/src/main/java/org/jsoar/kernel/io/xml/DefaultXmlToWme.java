/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 27, 2009
 */
package org.jsoar.kernel.io.xml;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Default implementation of {@link XmlToWme} interface
 * 
 * @author ray
 * @see XmlToWme
 * @see WmeFactory
 * @see DefaultWmeToXml
 */
public class DefaultXmlToWme implements XmlToWme
{
    private final WmeFactory<?> wmeFactory;
    
    /**
     * Construct an XML to WME converter for I/O, i.e. it uses an instance of
     * {@link InputOutput} to generate symbols and WMEs.
     * 
     * @param io the I/O interface to use
     * @return new converter
     */
    public static DefaultXmlToWme forInput(InputOutput io)
    {
        return new DefaultXmlToWme(io.asWmeFactory());
    }
    
    /**
     * Construct an XML to WME converter for RHS functions, i.e. it uses an
     * instance of {@link RhsFunctionContext} to generate symbols and WMEs.
     * 
     * @param rhsContext the RHS function context to use 
     * @return new converter
     */
    public static DefaultXmlToWme forRhsFunction(RhsFunctionContext rhsContext)
    {
        return new DefaultXmlToWme(rhsContext);
    }
    
    /**
     * Construct a new XML to WME converter using the given factory to construct
     * new symbols and WMEs.
     * 
     * @param wmeFactory the WME factory to use
     */
    public DefaultXmlToWme(WmeFactory<?> wmeFactory)
    {
        this.wmeFactory = wmeFactory;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.xml.XmlToWme#fromXml(org.w3c.dom.Element)
     */
    public Identifier fromXml(Element element)
    {
        return fromXmlInternal(element);
    }
    
    private void addAttributes(Element element, Identifier targetId)
    {
        assert element != null;
        assert targetId != null;

        final SymbolFactory syms = wmeFactory.getSymbols();
        final NamedNodeMap attrs = element.getAttributes();
        final int attrsLength = attrs.getLength();
        if(attrsLength == 0)
        {
            return;
        }
        
        final Identifier attrsId = syms.createIdentifier('a');
        wmeFactory.addWme(targetId, syms.createString(DefaultWmeToXml.ATTRS), attrsId);
        
        for(int i = 0; i < attrsLength; ++i)
        {
            final Attr attr = (Attr) attrs.item(i);
            
            wmeFactory.addWme(attrsId, 
                              syms.createString(attr.getName()), 
                              syms.createString(attr.getValue()));
        }
    }
    
    private void addTextWme(Identifier targetId, String text)
    {
        if(text != null && text.length() != 0)
        {
            final SymbolFactory syms = wmeFactory.getSymbols();
            wmeFactory.addWme(targetId, syms.createString(DefaultWmeToXml.TEXT), syms.createString(text));
        }
    }
    
    private Identifier fromXmlInternal(Element element)
    {
        final SymbolFactory syms = wmeFactory.getSymbols();
        final Identifier targetId = syms.createIdentifier(Symbols.getFirstLetter(element.getTagName()));
        String text = "";
        Identifier lastChildId = null;
        for(Node node = element.getFirstChild(); node != null; node = node.getNextSibling())
        {
            if(node instanceof Text)
            {
                text += ((Text) node).getData();
            }
            else if(node instanceof Element)
            {
                final Element kid = (Element) node;
                final String tagName = kid.getTagName();
                final Symbol attribute = syms.createString(tagName);
                final Identifier kidId = fromXmlInternal(kid);
                wmeFactory.addWme(targetId, attribute, kidId);
                
                // Create ^/next link from last element to this one so we can preserve 
                // the order or the original xml in working memory
                if(lastChildId != null)
                {
                    wmeFactory.addWme(lastChildId, syms.createString(DefaultWmeToXml.NEXT), kidId);
                }
                lastChildId = kidId;
            }
        }
        
        addAttributes(element, targetId);
        addTextWme(targetId, text.trim());
        
        return targetId;
    }
}
