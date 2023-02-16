/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 27, 2009
 */
package org.jsoar.kernel.io.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.XmlTools;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A transform from working memory to SoarTech's working memory XML format.
 * 
 * @author ray
 * @see SoarTechXmlToWme
 */
public class SoarTechWmeToXml
{
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String INTEGER = "integer";
    public static final String DOUBLE = "double";
    public static final String LINK = "link";
    public static final String LINK_ID = "link-id";
    
    private static final DocumentBuilder BUILDER = XmlTools.createDocumentBuilder();
    
    private final Map<Identifier, Element> idMap = new HashMap<>();
    
    /**
     * Convert a working memory tree to an XML document starting at the given
     * root.
     * 
     * @param root the root identifier to start at
     * @param rootName the name of the document element
     * @return an XML document representing the working memory
     */
    public Document toXml(Identifier root, String rootName)
    {
        final Document doc = BUILDER.getDOMImplementation().createDocument(null, rootName, null);
        toXml(root, doc.getDocumentElement());
        return doc;
    }
    
    /**
     * Convert working memory to XML starting at the given root identifier
     * and the given root element
     * 
     * @param root the root identifier
     * @param element the root XML element to write to
     * @return element
     */
    public Element toXml(Identifier root, Element element)
    {
        idMap.put(root, element);
        
        final Document doc = element.getOwnerDocument();
        for(final Iterator<Wme> it = root.getWmes(); it.hasNext();)
        {
            final Wme wme = it.next();
            
            final Element wmeElement = doc.createElement(wme.getAttribute().toString());
            element.appendChild(wmeElement);
            
            final Identifier idValue = wme.getValue().asIdentifier();
            if(idValue != null)
            {
                Element valueElement = idMap.get(idValue);
                if(valueElement != null)
                {
                    wmeElement.setAttribute(LINK, idValue.toString());
                    valueElement.setAttribute(LINK_ID, idValue.toString());
                }
                else
                {
                    toXml(idValue, wmeElement);
                }
            }
            else
            {
                writeNonIdValue(wme.getValue(), wmeElement);
            }
        }
        return element;
    }
    
    private void writeNonIdValue(Symbol value, Element element)
    {
        if(value.asDouble() != null)
        {
            element.setAttribute(TYPE, DOUBLE);
        }
        else if(value.asInteger() != null)
        {
            element.setAttribute(TYPE, INTEGER);
        }
        element.setTextContent(value.toString());
    }
}
