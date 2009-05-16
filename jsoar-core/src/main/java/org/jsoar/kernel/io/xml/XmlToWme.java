/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 27, 2009
 */
package org.jsoar.kernel.io.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * @author ray
 */
public class XmlToWme
{
    private static final Log logger = LogFactory.getLog(XmlToWme.class);

    private final SymbolFactory syms;
    private final XmlWmeFactory wmeFactory;
    
    /**
     * @param syms
     * @param wmeFactory
     */
    public XmlToWme(SymbolFactory syms, XmlWmeFactory wmeFactory)
    {
        this.syms = syms;
        this.wmeFactory = wmeFactory;
    }

    public Symbol fromXml(Element element)
    {
        return fromXmlInternal(element);
    }
    
    private void addAttributes(Element element, Identifier targetId)
    {
        assert element != null;
        assert targetId != null;
        
        final NamedNodeMap attrs = element.getAttributes();
        final int attrsLength = attrs.getLength();
        if(attrsLength == 0)
        {
            return;
        }
        
        final Identifier attrsId = syms.createIdentifier('a');
        wmeFactory.addWme(targetId, syms.createString(WmeToXml.ATTRS), attrsId);
        
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
            wmeFactory.addWme(targetId, syms.createString(WmeToXml.TEXT), syms.createString(text));
        }
    }
    
    private Identifier fromXmlInternal(Element element)
    {
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
                    wmeFactory.addWme(lastChildId, syms.createString(WmeToXml.NEXT), kidId);
                }
                lastChildId = kidId;
            }
        }
        
        addAttributes(element, targetId);
        addTextWme(targetId, text.trim());
        
        return targetId;
    }
}
