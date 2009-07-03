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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.XmlTools;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Convert SoarTech's XML working memory format to working memory.
 * 
 * @author ray
 * @see XmlToWme
 * @see InputXmlWmeFactory
 * @see RhsFunctionXmlWmeFactory
 */
public class SoarTechXmlToWme implements XmlToWme
{
    private static final Log logger = LogFactory.getLog(SoarTechXmlToWme.class);
    
    private final SymbolFactory syms;
    private final XmlWmeFactory wmeFactory;
    
    private final Map<String, Symbol> linkMap = new HashMap<String, Symbol>(); 
    private final List<Link> links = new ArrayList<Link>();
    
    /**
     * @param syms
     * @param wmeFactory
     */
    public SoarTechXmlToWme(SymbolFactory syms, XmlWmeFactory wmeFactory)
    {
        this.syms = syms;
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
        final String type = element.getAttribute(SoarTechWmeToXml.TYPE);
        if(type.length() == 0 && XmlTools.getFirstChild(element) != null)
        {
            return fromXmlInternal(element, null);
        }
        else if(SoarTechWmeToXml.DOUBLE.equals(type))
        {
            return syms.createDouble(Double.valueOf(element.getTextContent()));
        }
        else if(SoarTechWmeToXml.INTEGER.equals(type))
        {
            return syms.createInteger(Integer.valueOf(element.getTextContent()));
        }
        else
        {
            return syms.createString(element.getTextContent());
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
