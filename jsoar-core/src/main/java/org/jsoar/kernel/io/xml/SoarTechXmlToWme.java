/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 27, 2009
 */
package org.jsoar.kernel.io.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.Arguments;
import org.jsoar.util.XmlTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Convert SoarTech's XML working memory format to working memory.
 * 
 * @author ray
 * @see XmlToWme
 * @see WmeFactory
 */
public class SoarTechXmlToWme implements XmlToWme
{
    private static final Logger logger = LoggerFactory.getLogger(SoarTechXmlToWme.class);
    
    private final WmeFactory<?> wmeFactory;
    
    private final Map<String, Symbol> linkMap = new HashMap<String, Symbol>(); 
    private final List<Link> links = new ArrayList<Link>();
    
    /**
     * Construct an XML to WME converter for I/O, i.e. it uses an instance of
     * {@link InputOutput} to generate symbols and WMEs.
     * 
     * @param io the I/O interface to use
     * @return new converter
     */
    public static SoarTechXmlToWme forInput(InputOutput io)
    {
        return new SoarTechXmlToWme(io.asWmeFactory());
    }
    
    /**
     * Construct an XML to WME converter for RHS functions, i.e. it uses an
     * instance of {@link RhsFunctionContext} to generate symbols and WMEs.
     * 
     * @param rhsContext the RHS function context to use 
     * @return new converter
     */
    public static SoarTechXmlToWme forRhsFunction(RhsFunctionContext rhsContext)
    {
        return new SoarTechXmlToWme(rhsContext);
    }
    
    /**
     * Construct a new XML to WME converter using the given factory interface to
     * generate symbols and WMEs
     * 
     * @param wmeFactory the WME factory
     */
    public SoarTechXmlToWme(WmeFactory<?> wmeFactory)
    {
        Arguments.checkNotNull(wmeFactory, "wmeFactory");
        this.wmeFactory = wmeFactory;
    }
    
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.xml.XmlToWme#fromXml(org.w3c.dom.Element)
     */
    public Identifier fromXml(Element element)
    {
        return fromXml(element, null);
    }
    
    public Identifier fromXml(Element element, Identifier targetId)
    {
        final Identifier result = fromXmlInternal(element, targetId);
        
        for(Link link : links)
        {
            final Symbol target = linkMap.get(link.linkTo);
            if(target != null)
            {
                wmeFactory.addWme(link.from, link.attribute, target);
            }
            else
            {
                logger.error("Unknown link target '" + link.linkTo + "'");
            }
        }
        
        links.clear();
        linkMap.clear();
        return result;
    }
    
    private Identifier fromXmlInternal(Element element, Identifier targetId)
    {
        final SymbolFactory syms = wmeFactory.getSymbols();
        
        if(targetId == null)
        {
            targetId = syms.createIdentifier(element.getTagName().charAt(0));
        }
        
        for(Node node = element.getFirstChild(); node != null; node = node.getNextSibling())
        {
            if(!(node instanceof Element)) continue;
            
            final Element kid = (Element) node;
            final String linkTo = kid.getAttribute(SoarTechWmeToXml.LINK);
            final Symbol attribute = syms.createString(kid.getTagName());
            if(linkTo.length() == 0)
            {
                final Symbol value = getValue(kid);
                wmeFactory.addWme(targetId, attribute, value);
                
                final String link = kid.getAttribute(SoarTechWmeToXml.LINK_ID);
                if(link.length() != 0)
                {
                    linkMap.put(link, value);
                }
            }
            else
            {
                links.add(new Link(targetId, attribute, linkTo));
            }
        }
        return targetId;
    }
    
    private Symbol getValue(Element element)
    {
        final SymbolFactory syms = wmeFactory.getSymbols();
        final String type = element.getAttribute(SoarTechWmeToXml.TYPE);
        if(type.length() == 0 && XmlTools.getFirstChild(element) != null)
        {
            return fromXmlInternal(element, null);
        }
        
        final String value;
        if(element.hasAttribute(SoarTechWmeToXml.VALUE))
        {
            value = element.getAttribute(SoarTechWmeToXml.VALUE);
        }
        else
        {
            value = element.getTextContent();
        }
        
        if(SoarTechWmeToXml.DOUBLE.equals(type))
        {
            return syms.createDouble(Double.valueOf(value));
        }
        else if(SoarTechWmeToXml.INTEGER.equals(type))
        {
            return syms.createInteger(Long.valueOf(value));
        }
        else
        {
            return syms.createString(value);
        }
    }
    
    private static class Link
    {
        private final Identifier from;
        private final Symbol attribute;
        private final String linkTo;
        
        public Link(Identifier from, Symbol attribute, String linkTo)
        {
            this.from = from;
            this.attribute = attribute;
            this.linkTo = linkTo;
        }
    }
}
