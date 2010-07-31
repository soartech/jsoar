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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.XmlTools;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * A transform from working memory to a naive XML format.
 * 
 * @author ray
 */
public class DefaultWmeToXml
{
    public static final String ATTRS = "/attrs";
    public static final String TEXT = "/text";
    public static final String NEXT = "/next";
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultXmlToWme.class);
    private static final DocumentBuilder builder = XmlTools.createDocumentBuilder();
    
    private final Map<Identifier, Element> idMap = new HashMap<Identifier, Element>();
    
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
        final Document doc = builder.getDOMImplementation().createDocument(null, rootName, null);
        toXml(root, doc.getDocumentElement());
        return doc;
    }
    
    private void processAttrs(Wme attrWme, Element element)
    {
        final Identifier idValue = attrWme.getValue().asIdentifier();
        if(idValue != null)
        {
            
            for(Iterator<Wme> it = idValue.getWmes(); it.hasNext();)
            {
                final Wme child = it.next();
                final String attr = child.getAttribute().toString();
                if(ATTRS.equals(attr))
                {
                    processAttrs(child, element);
                }
                else
                {
                    element.setAttribute(attr, child.getValue().toString());
                }
            }
        }
        else
        {
            logger.warn("Don't know what to do with " + ATTRS + " element with non-id value: " + attrWme);
        }
    }
    
    private void processText(Wme textWme, Element element)
    {
        final Text text = element.getOwnerDocument().createTextNode(textWme.getValue().toString());
        element.appendChild(text);
    }
    
    private void processNormalWme(Wme wme, Element element)
    {
        final Document doc = element.getOwnerDocument();
        
        // Create a new XML element for the WME
        final Element wmeElement = doc.createElement(wme.getAttribute().toString());
        element.appendChild(wmeElement);
        
        //  If it's an ID, process sub-structure
        final Identifier idValue = wme.getValue().asIdentifier();
        if(idValue != null)
        {
            Element valueElement = idMap.get(idValue);
            if(valueElement != null)
            {
                // TODO what should we do here? XLink?
                logger.warn("Don't know what to do with non-tree memory structure: " + wme);
                wmeElement.setTextContent(idValue.toString());
            }
            else
            {
                toXml(idValue, wmeElement);
            }
        }
        else
        {
            // Otherwise, just convert the value to text and put it in the
            // element's text content.
            final String value = wme.getValue().toString();
            wmeElement.setTextContent(value);
        }        
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
        
        for(final Iterator<Wme> it = root.getWmes(); it.hasNext();)
        {
            final Wme wme = it.next();
            
            final String attr = wme.getAttribute().toString();
            if(ATTRS.equals(attr))
            {
                processAttrs(wme, element);
            }
            else if(TEXT.equals(attr))
            {
                processText(wme, element);
            }
            else if(NEXT.equals(attr))
            {
                // TODO handle ^/next attributes
            }
            else
            {
                processNormalWme(wme, element);
            }
            
        }
        return element;
    }
}
